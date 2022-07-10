package net.caffeinemc.sodium.render.chunk.draw;

import com.mojang.blaze3d.systems.RenderSystem;
import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import net.caffeinemc.gfx.api.array.VertexArrayDescription;
import net.caffeinemc.gfx.api.array.VertexArrayResourceBinding;
import net.caffeinemc.gfx.api.array.attribute.VertexAttributeBinding;
import net.caffeinemc.gfx.api.buffer.Buffer;
import net.caffeinemc.gfx.api.buffer.MappedBufferFlags;
import net.caffeinemc.gfx.api.device.RenderDevice;
import net.caffeinemc.gfx.api.device.commands.RenderCommandList;
import net.caffeinemc.gfx.api.pipeline.Pipeline;
import net.caffeinemc.gfx.api.pipeline.PipelineState;
import net.caffeinemc.gfx.api.shader.Program;
import net.caffeinemc.gfx.api.shader.ShaderDescription;
import net.caffeinemc.gfx.api.shader.ShaderType;
import net.caffeinemc.gfx.util.buffer.DualStreamingBuffer;
import net.caffeinemc.gfx.util.buffer.SequenceBuilder;
import net.caffeinemc.gfx.util.buffer.SequenceIndexBuffer;
import net.caffeinemc.gfx.util.buffer.StreamingBuffer;
import net.caffeinemc.sodium.SodiumClientMod;
import net.caffeinemc.sodium.render.chunk.RenderSection;
import net.caffeinemc.sodium.render.chunk.passes.ChunkRenderPass;
import net.caffeinemc.sodium.render.chunk.passes.ChunkRenderPassManager;
import net.caffeinemc.sodium.render.chunk.shader.ChunkShaderBindingPoints;
import net.caffeinemc.sodium.render.chunk.shader.ChunkShaderInterface;
import net.caffeinemc.sodium.render.chunk.state.ChunkPassModel;
import net.caffeinemc.sodium.render.chunk.state.ChunkRenderBounds;
import net.caffeinemc.sodium.render.shader.ShaderConstants;
import net.caffeinemc.sodium.render.shader.ShaderLoader;
import net.caffeinemc.sodium.render.shader.ShaderParser;
import net.caffeinemc.sodium.render.terrain.format.TerrainMeshAttribute;
import net.caffeinemc.sodium.render.terrain.format.TerrainVertexType;
import net.caffeinemc.sodium.render.terrain.quad.properties.ChunkMeshFace;
import net.caffeinemc.sodium.util.MathUtil;
import net.caffeinemc.sodium.util.TextureUtil;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import org.joml.Matrix4f;
import org.lwjgl.system.MemoryUtil;

// TODO: abstract buffer targets, abstract VAO creation, supply shader identifiers
public abstract class AbstractMdChunkRenderer<B extends AbstractMdChunkRenderer.MdChunkRenderBatch> extends AbstractChunkRenderer {
    public static final int TRANSFORM_STRUCT_STRIDE = 4 * Float.BYTES;
    public static final int CAMERA_MATRICES_SIZE = 192;
    public static final int FOG_PARAMETERS_SIZE = 32;
    
    protected final ChunkRenderPassManager renderPassManager;
    protected final Pipeline<ChunkShaderInterface, BufferTarget>[] pipelines;
    
    protected final StreamingBuffer uniformBufferCameraMatrices;
    protected final StreamingBuffer uniformBufferChunkTransforms;
    protected final StreamingBuffer uniformBufferFogParameters;
    protected final SequenceIndexBuffer indexBuffer;
    
    protected Collection<B>[] renderLists;
    
    public AbstractMdChunkRenderer(
            RenderDevice device,
            ChunkRenderPassManager renderPassManager,
            TerrainVertexType vertexType
    ) {
        super(device);
    
        this.renderPassManager = renderPassManager;
    
        //noinspection unchecked
        this.pipelines = new Pipeline[renderPassManager.getRenderPassCount()];
    
        // construct all pipelines for current render passes now
        var vertexFormat = vertexType.getCustomVertexFormat();
        var vertexArray = new VertexArrayDescription<>(
                BufferTarget.values(),
                List.of(new VertexArrayResourceBinding<>(
                        BufferTarget.VERTICES,
                        new VertexAttributeBinding[] {
                                new VertexAttributeBinding(
                                        ChunkShaderBindingPoints.ATTRIBUTE_POSITION,
                                        vertexFormat.getAttribute(
                                                TerrainMeshAttribute.POSITION)
                                ),
                                new VertexAttributeBinding(
                                        ChunkShaderBindingPoints.ATTRIBUTE_COLOR,
                                        vertexFormat.getAttribute(
                                                TerrainMeshAttribute.COLOR)
                                ),
                                new VertexAttributeBinding(
                                        ChunkShaderBindingPoints.ATTRIBUTE_BLOCK_TEXTURE,
                                        vertexFormat.getAttribute(
                                                TerrainMeshAttribute.BLOCK_TEXTURE)
                                ),
                                new VertexAttributeBinding(
                                        ChunkShaderBindingPoints.ATTRIBUTE_LIGHT_TEXTURE,
                                        vertexFormat.getAttribute(
                                                TerrainMeshAttribute.LIGHT_TEXTURE)
                                )
                        }
                ))
        );
    
        for (ChunkRenderPass renderPass : renderPassManager.getAllRenderPasses()) {
            var constants = this.addAdditionalShaderConstants(getBaseShaderConstants(renderPass, vertexType)).build();
        
            var vertShader = ShaderParser.parseSodiumShader(
                    ShaderLoader.MINECRAFT_ASSETS,
                    new Identifier("sodium", "terrain/terrain_opaque.vert"),
                    constants
            );
            var fragShader = ShaderParser.parseSodiumShader(
                    ShaderLoader.MINECRAFT_ASSETS,
                    new Identifier("sodium", "terrain/terrain_opaque.frag"),
                    constants
            );
        
            var desc = ShaderDescription.builder()
                                        .addShaderSource(ShaderType.VERTEX, vertShader)
                                        .addShaderSource(ShaderType.FRAGMENT, fragShader)
                                        .build();
        
            Program<ChunkShaderInterface> program = this.device.createProgram(desc, ChunkShaderInterface::new);
            Pipeline<ChunkShaderInterface, BufferTarget> pipeline = this.device.createPipeline(
                    renderPass.getPipelineDescription(),
                    program,
                    vertexArray
            );
        
            this.pipelines[renderPass.getId()] = pipeline;
        }
        
        // Set up buffers
        int maxInFlightFrames = SodiumClientMod.options().advanced.cpuRenderAheadLimit + 1;
        int uboAlignment = device.properties().values.uniformBufferOffsetAlignment;
        int totalPasses = renderPassManager.getRenderPassCount();
    
        this.uniformBufferCameraMatrices = new DualStreamingBuffer(
                device,
                uboAlignment,
                MathUtil.align(CAMERA_MATRICES_SIZE, uboAlignment) * totalPasses,
                maxInFlightFrames,
                EnumSet.of(MappedBufferFlags.EXPLICIT_FLUSH)
        );
        this.uniformBufferChunkTransforms = new DualStreamingBuffer(
                device,
                uboAlignment,
                1048576, // start with 1 MiB and expand from there if needed
                maxInFlightFrames,
                EnumSet.of(MappedBufferFlags.EXPLICIT_FLUSH)
        );
        this.uniformBufferFogParameters = new DualStreamingBuffer(
                device,
                uboAlignment,
                MathUtil.align(FOG_PARAMETERS_SIZE, uboAlignment) * totalPasses,
                maxInFlightFrames,
                EnumSet.of(MappedBufferFlags.EXPLICIT_FLUSH)
        );
        this.indexBuffer = new SequenceIndexBuffer(device, SequenceBuilder.QUADS_INT);
    }
    
    protected static ShaderConstants.Builder getBaseShaderConstants(ChunkRenderPass pass, TerrainVertexType vertexType) {
        var constants = ShaderConstants.builder();
        
        if (pass.isCutout()) {
            constants.add("ALPHA_CUTOFF", String.valueOf(pass.getAlphaCutoff()));
        }
        
        if (!MathHelper.approximatelyEquals(vertexType.getVertexRange(), 1.0f)) {
            constants.add("VERT_SCALE", String.valueOf(vertexType.getVertexRange()));
        }
        
        return constants;
    }
    
    protected ShaderConstants.Builder addAdditionalShaderConstants(ShaderConstants.Builder constants) {
        return constants; // NOOP, override if needed
    }
    
    //// RENDER METHODS
    
    @Override
    public void render(ChunkRenderPass renderPass, ChunkRenderMatrices matrices, int frameIndex) {
        // make sure a render list was created for this pass, if any
        if (this.renderLists == null) {
            return;
        }
        
        int passId = renderPass.getId();
        if (passId < 0 || this.renderLists.length < passId) {
            return;
        }
        
        var renderList = this.renderLists[passId];
        if (renderList == null) {
            return;
        }
        
        // if the render list exists, the pipeline probably exists (unless a new render pass was added without a reload)
        Pipeline<ChunkShaderInterface, BufferTarget> pipeline = this.pipelines[passId];
        
        this.device.usePipeline(pipeline, (commandList, programInterface, pipelineState) -> {
            this.setupPerRenderList(renderPass, matrices, frameIndex, pipeline, commandList, programInterface, pipelineState);
            
            for (B batch : renderList) {
                this.setupPerBatch(renderPass, matrices, frameIndex, pipeline, commandList, programInterface, pipelineState, batch);
                
                this.issueDraw(renderPass, matrices, frameIndex, pipeline, commandList, programInterface, pipelineState, batch);
            }
        });
    }
    
    //// OVERRIDABLE RENDERING METHODS
    
    protected void setupPerRenderList(
            ChunkRenderPass renderPass,
            ChunkRenderMatrices matrices,
            int frameIndex,
            Pipeline<ChunkShaderInterface, BufferTarget> pipeline,
            RenderCommandList<BufferTarget> commandList,
            ChunkShaderInterface programInterface,
            PipelineState pipelineState
    ) {
        this.setupTextures(renderPass, pipelineState);
        this.setupUniforms(matrices, programInterface, pipelineState, frameIndex);
        
        commandList.bindElementBuffer(this.indexBuffer.getBuffer());
    }
    
    protected void setupPerBatch(
            ChunkRenderPass renderPass,
            ChunkRenderMatrices matrices,
            int frameIndex,
            Pipeline<ChunkShaderInterface, BufferTarget> pipeline,
            RenderCommandList<BufferTarget> commandList,
            ChunkShaderInterface programInterface,
            PipelineState pipelineState,
            B batch
    ) {
        commandList.bindVertexBuffer(
                BufferTarget.VERTICES,
                batch.getVertexBuffer(),
                0,
                batch.getVertexStride()
        );
    }
    
    protected abstract void issueDraw(
            ChunkRenderPass renderPass,
            ChunkRenderMatrices matrices,
            int frameIndex,
            Pipeline<ChunkShaderInterface, BufferTarget> pipeline,
            RenderCommandList<BufferTarget> commandList,
            ChunkShaderInterface programInterface,
            PipelineState pipelineState,
            B batch
    );
    
    protected void setupTextures(ChunkRenderPass pass, PipelineState pipelineState) {
        pipelineState.bindTexture(
                0,
                TextureUtil.getBlockAtlasTexture(),
                pass.isMipped() ? this.blockTextureMippedSampler : this.blockTextureSampler
        );
        pipelineState.bindTexture(1, TextureUtil.getLightTexture(), this.lightTextureSampler);
    }
    
    protected void setupUniforms(
            ChunkRenderMatrices renderMatrices,
            ChunkShaderInterface programInterface,
            PipelineState state,
            int frameIndex
    ) {
        StreamingBuffer.WritableSection matricesSection = this.uniformBufferCameraMatrices.getSection(frameIndex, CAMERA_MATRICES_SIZE, true);
        ByteBuffer matricesView = matricesSection.getView();
        long matricesPtr = MemoryUtil.memAddress(matricesView);
        
        renderMatrices.projection().getToAddress(matricesPtr);
        renderMatrices.modelView().getToAddress(matricesPtr + 64);
        
        Matrix4f mvpMatrix = new Matrix4f();
        mvpMatrix.set(renderMatrices.projection());
        mvpMatrix.mul(renderMatrices.modelView());
        mvpMatrix.getToAddress(matricesPtr + 128);
        matricesView.position(matricesView.position() + CAMERA_MATRICES_SIZE);
        
        matricesSection.flushPartial();
        
        state.bindBufferBlock(
                programInterface.uniformCameraMatrices,
                this.uniformBufferCameraMatrices.getBufferObject(),
                matricesSection.getDeviceOffset(),
                matricesSection.getView().capacity()
        );
        
        StreamingBuffer.WritableSection fogParamsSection = this.uniformBufferFogParameters.getSection(frameIndex, FOG_PARAMETERS_SIZE, true);
        ByteBuffer fogParamsView = fogParamsSection.getView();
        long fogParamsPtr = MemoryUtil.memAddress(fogParamsView);
        
        float[] paramFogColor = RenderSystem.getShaderFogColor();
        MemoryUtil.memPutFloat(fogParamsPtr + 0, paramFogColor[0]);
        MemoryUtil.memPutFloat(fogParamsPtr + 4, paramFogColor[1]);
        MemoryUtil.memPutFloat(fogParamsPtr + 8, paramFogColor[2]);
        MemoryUtil.memPutFloat(fogParamsPtr + 12, paramFogColor[3]);
        MemoryUtil.memPutFloat(fogParamsPtr + 16, RenderSystem.getShaderFogStart());
        MemoryUtil.memPutFloat(fogParamsPtr + 20, RenderSystem.getShaderFogEnd());
        MemoryUtil.memPutInt(  fogParamsPtr + 24, RenderSystem.getShaderFogShape().getId());
        fogParamsView.position(fogParamsView.position() + FOG_PARAMETERS_SIZE);
        
        fogParamsSection.flushPartial();
        
        state.bindBufferBlock(
                programInterface.uniformFogParameters,
                this.uniformBufferFogParameters.getBufferObject(),
                fogParamsSection.getDeviceOffset(),
                fogParamsSection.getView().capacity()
        );
    }
    
    //// UTILITY METHODS
    
    protected static int getMaxSectionFaces(SortedChunkLists list) {
        int faces = 0;
    
        for (SortedChunkLists.RegionBucket regionBucket : list.unsortedRegionBuckets()) {
            for (RenderSection section : regionBucket.unsortedSections()) {
                for (ChunkPassModel model : section.getData().models) {
                    if (model == null) {
                        continue;
                    }
                    // each bit set represents a model, so we can just count the set bits
                    faces += Integer.bitCount(model.getVisibilityBits());
                }
            }
        }
        
        return faces;
    }
    
    protected static float getCameraTranslation(int chunkBlockPos, int cameraBlockPos, float cameraPos) {
        return (chunkBlockPos - cameraBlockPos) - cameraPos;
    }
    
    protected static int calculateVisibilityFlags(ChunkRenderBounds bounds, ChunkCameraContext camera) {
        int flags = ChunkMeshFace.UNASSIGNED_BITS;
        
        if (camera.posY > bounds.y1) {
            flags |= ChunkMeshFace.UP_BITS;
        }
        
        if (camera.posY < bounds.y2) {
            flags |= ChunkMeshFace.DOWN_BITS;
        }
        
        if (camera.posX > bounds.x1) {
            flags |= ChunkMeshFace.EAST_BITS;
        }
        
        if (camera.posX < bounds.x2) {
            flags |= ChunkMeshFace.WEST_BITS;
        }
        
        if (camera.posZ > bounds.z1) {
            flags |= ChunkMeshFace.SOUTH_BITS;
        }
        
        if (camera.posZ < bounds.z2) {
            flags |= ChunkMeshFace.NORTH_BITS;
        }
        
        return flags;
    }
    
    //// OVERRIDABLE BATCH
    
    protected static class MdChunkRenderBatch {
        protected final Buffer vertexBuffer;
        protected final int vertexStride;
        protected final int commandCount;
        protected final long transformBufferOffset;
    
        public MdChunkRenderBatch(
                Buffer vertexBuffer,
                int vertexStride,
                int commandCount,
                long transformBufferOffset
        ) {
            this.vertexBuffer = vertexBuffer;
            this.vertexStride = vertexStride;
            this.commandCount = commandCount;
            this.transformBufferOffset = transformBufferOffset;
        }
    
        public Buffer getVertexBuffer() {
            return this.vertexBuffer;
        }
    
        public int getVertexStride() {
            return this.vertexStride;
        }
    
        public int getCommandCount() {
            return this.commandCount;
        }
    
        public long getTransformsBufferOffset() {
            return this.transformBufferOffset;
        }
    }
    
    //// OVERRIDABLE BUFFER INFO
    
    @Override
    public int getDeviceBufferObjects() {
        return 4;
    }
    
    @Override
    public long getDeviceUsedMemory() {
        return this.uniformBufferCameraMatrices.getDeviceUsedMemory() +
               this.uniformBufferChunkTransforms.getDeviceUsedMemory() +
               this.uniformBufferFogParameters.getDeviceUsedMemory() +
               this.indexBuffer.getDeviceUsedMemory();
    }
    
    @Override
    public long getDeviceAllocatedMemory() {
        return this.uniformBufferCameraMatrices.getDeviceAllocatedMemory() +
               this.uniformBufferChunkTransforms.getDeviceAllocatedMemory() +
               this.uniformBufferFogParameters.getDeviceAllocatedMemory() +
               this.indexBuffer.getDeviceAllocatedMemory();
    }
    
    //// MISC METHODS
    
    @Override
    public void delete() {
        super.delete();
        this.uniformBufferCameraMatrices.delete();
        this.uniformBufferChunkTransforms.delete();
        this.uniformBufferFogParameters.delete();
        this.indexBuffer.delete();
    }
}
