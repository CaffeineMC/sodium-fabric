package net.caffeinemc.sodium.render.chunk.draw;

import com.mojang.blaze3d.systems.RenderSystem;
import java.nio.ByteBuffer;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import net.caffeinemc.gfx.api.array.VertexArrayDescription;
import net.caffeinemc.gfx.api.array.VertexArrayResourceBinding;
import net.caffeinemc.gfx.api.array.attribute.VertexAttributeBinding;
import net.caffeinemc.gfx.api.buffer.Buffer;
import net.caffeinemc.gfx.api.buffer.MappedBufferFlags;
import net.caffeinemc.gfx.api.device.RenderDevice;
import net.caffeinemc.gfx.api.pipeline.Pipeline;
import net.caffeinemc.gfx.api.pipeline.PipelineState;
import net.caffeinemc.gfx.api.shader.Program;
import net.caffeinemc.gfx.api.shader.ShaderDescription;
import net.caffeinemc.gfx.api.shader.ShaderType;
import net.caffeinemc.gfx.api.types.ElementFormat;
import net.caffeinemc.gfx.api.types.PrimitiveType;
import net.caffeinemc.gfx.util.buffer.DualStreamingBuffer;
import net.caffeinemc.gfx.util.buffer.SequenceBuilder;
import net.caffeinemc.gfx.util.buffer.SequenceIndexBuffer;
import net.caffeinemc.gfx.util.buffer.StreamingBuffer;
import net.caffeinemc.sodium.SodiumClientMod;
import net.caffeinemc.sodium.render.buffer.VertexRange;
import net.caffeinemc.sodium.render.chunk.RenderSection;
import net.caffeinemc.sodium.render.chunk.passes.ChunkRenderPass;
import net.caffeinemc.sodium.render.chunk.passes.ChunkRenderPassManager;
import net.caffeinemc.sodium.render.chunk.region.RenderRegion;
import net.caffeinemc.sodium.render.chunk.shader.ChunkShaderBindingPoints;
import net.caffeinemc.sodium.render.chunk.shader.ChunkShaderInterface;
import net.caffeinemc.sodium.render.chunk.state.ChunkPassModel;
import net.caffeinemc.sodium.render.chunk.state.ChunkRenderBounds;
import net.caffeinemc.sodium.render.chunk.state.UploadedChunkGeometry;
import net.caffeinemc.sodium.render.shader.ShaderConstants;
import net.caffeinemc.sodium.render.shader.ShaderLoader;
import net.caffeinemc.sodium.render.shader.ShaderParser;
import net.caffeinemc.sodium.render.terrain.format.TerrainMeshAttribute;
import net.caffeinemc.sodium.render.terrain.format.TerrainVertexType;
import net.caffeinemc.sodium.render.terrain.quad.properties.ChunkMeshFace;
import net.caffeinemc.sodium.util.MathUtil;
import net.caffeinemc.sodium.util.TextureUtil;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.util.math.MathHelper;
import org.joml.Matrix4f;
import org.lwjgl.system.MemoryUtil;

public class MdiChunkRenderer extends AbstractChunkRenderer {
    public static final int COMMAND_STRUCT_STRIDE = 5 * Integer.BYTES;
    public static final int INSTANCE_STRUCT_STRIDE = 4 * Float.BYTES;
    public static final int CAMERA_MATRICES_SIZE = 192;
    public static final int FOG_PARAMETERS_SIZE = 32;
    public static final int INSTANCE_DATA_SIZE = RenderRegion.REGION_SIZE * INSTANCE_STRUCT_STRIDE;

    protected final ChunkRenderPassManager renderPassManager;

    protected final Pipeline<ChunkShaderInterface, BufferTarget>[] pipelines;

    protected final StreamingBuffer uniformBufferCameraMatrices;
    protected final StreamingBuffer uniformBufferInstanceData;
    protected final StreamingBuffer uniformBufferFogParameters;
    protected final StreamingBuffer commandBuffer;
    protected final SequenceIndexBuffer indexBuffer;

    private Collection<MdiChunkRenderBatch>[] renderLists;

    public MdiChunkRenderer(
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
            var constants = getShaderConstants(renderPass, vertexType);

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
        this.uniformBufferInstanceData = new DualStreamingBuffer(
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
        this.commandBuffer = new DualStreamingBuffer(
                device,
                1,
                1048576, // start with 1 MiB and expand from there if needed
                maxInFlightFrames,
                EnumSet.of(MappedBufferFlags.EXPLICIT_FLUSH)
        );
        this.indexBuffer = new SequenceIndexBuffer(device, SequenceBuilder.QUADS_INT);
    }

    @Override
    public int getDeviceBufferObjects() {
        return 5;
    }

    @Override
    public long getDeviceUsedMemory() {
        return this.uniformBufferCameraMatrices.getDeviceUsedMemory() +
               this.uniformBufferInstanceData.getDeviceUsedMemory() +
               this.uniformBufferFogParameters.getDeviceUsedMemory() +
               this.commandBuffer.getDeviceUsedMemory() +
               this.indexBuffer.getDeviceUsedMemory();
    }

    @Override
    public long getDeviceAllocatedMemory() {
        return this.uniformBufferCameraMatrices.getDeviceAllocatedMemory() +
               this.uniformBufferInstanceData.getDeviceAllocatedMemory() +
               this.uniformBufferFogParameters.getDeviceAllocatedMemory() +
               this.commandBuffer.getDeviceAllocatedMemory() +
               this.indexBuffer.getDeviceAllocatedMemory();
    }

    @Override
    public void createRenderLists(SortedChunkLists chunks, ChunkCameraContext camera, int frameIndex) {
        if (chunks.isEmpty()) {
            this.renderLists = null;
            return;
        }

        ChunkRenderPass[] chunkRenderPasses = this.renderPassManager.getAllRenderPasses();
        int totalPasses = chunkRenderPasses.length;

        // setup buffers, resizing as needed
        int commandBufferPassSize = commandBufferPassSize(this.commandBuffer.getAlignment(), chunks);
        StreamingBuffer.WritableSection commandBufferSection = this.commandBuffer.getSection(
                frameIndex,
                commandBufferPassSize * totalPasses,
                false
        );
        ByteBuffer commandBufferSectionView = commandBufferSection.getView();
        long commandBufferSectionAddress = MemoryUtil.memAddress0(commandBufferSectionView);

        int instanceBufferPassSize = instanceBufferPassSize(this.uniformBufferInstanceData.getAlignment(), chunks);
        StreamingBuffer.WritableSection instanceBufferSection = this.uniformBufferInstanceData.getSection(
                frameIndex,
                instanceBufferPassSize * totalPasses,
                false
        );
        ByteBuffer instanceBufferSectionView = instanceBufferSection.getView();
        long instanceBufferSectionAddress = MemoryUtil.memAddress0(instanceBufferSectionView);
    
        int largestVertexIndex = 0;
        int commandBufferPosition = commandBufferSectionView.position();
        int instanceBufferPosition = instanceBufferSectionView.position();
    
        @SuppressWarnings("unchecked")
        Collection<MdiChunkRenderBatch>[] renderLists = new Collection[totalPasses];
    
        for (int passId = 0; passId < chunkRenderPasses.length; passId++) {
            ChunkRenderPass renderPass = chunkRenderPasses[passId];
            Deque<MdiChunkRenderBatch> renderList = new ArrayDeque<>(16); // just an estimate
    
            boolean reverseOrder = renderPass.isTranslucent();
            
            for (Iterator<SortedChunkLists.RegionBucket> regionIterator = chunks.sortedRegionBuckets(reverseOrder); regionIterator.hasNext(); ) {
                SortedChunkLists.RegionBucket regionBucket = regionIterator.next();
                
                int batchInstanceCount = 0;
                int batchCommandCount = 0;
            
                for (Iterator<RenderSection> sectionIterator = regionBucket.sortedSections(reverseOrder); sectionIterator.hasNext(); ) {
                    RenderSection section = sectionIterator.next();
                
                    UploadedChunkGeometry geometry = section.getGeometry();
                    if (geometry.models == null) {
                        continue;
                    }
                    
                    int baseVertex = geometry.segment.getOffset();
                
                    int visibility = calculateVisibilityFlags(section.getBounds(), camera);
                    
                    ChunkPassModel model = geometry.models[passId];
                    
                    if (model == null || (model.getVisibilityBits() & visibility) == 0) {
                        continue;
                    }
                
                    VertexRange[] modelParts = model.getModelParts();
                    for (int dir = 0; dir < modelParts.length; dir++) {
                        if ((visibility & (1 << dir)) == 0) {
                            continue;
                        }
                        
                        VertexRange modelPart = modelParts[dir];
                        
                        if (modelPart == null) {
                            continue;
                        }
                    
                        long ptr = commandBufferSectionAddress + commandBufferPosition;
                        MemoryUtil.memPutInt(ptr + 0, modelPart.indexCount());
                        MemoryUtil.memPutInt(ptr + 4, 1);
                        MemoryUtil.memPutInt(ptr + 8, 0);
                        MemoryUtil.memPutInt(ptr + 12, baseVertex + modelPart.firstVertex()); // baseVertex
                        MemoryUtil.memPutInt(ptr + 16, batchInstanceCount); // baseInstance
                        commandBufferPosition += COMMAND_STRUCT_STRIDE;
                        batchCommandCount++;
                    }
                    
                    // TODO: should only need instance buffer data written once or twice, not for every render pass
                
                    float x = getCameraTranslation(
                            ChunkSectionPos.getBlockCoord(section.getChunkX()),
                            camera.blockX,
                            camera.deltaX
                    );
                    float y = getCameraTranslation(
                            ChunkSectionPos.getBlockCoord(section.getChunkY()),
                            camera.blockY,
                            camera.deltaY
                    );
                    float z = getCameraTranslation(
                            ChunkSectionPos.getBlockCoord(section.getChunkZ()),
                            camera.blockZ,
                            camera.deltaZ
                    );
                
                    long ptr = instanceBufferSectionAddress + instanceBufferPosition;
                    MemoryUtil.memPutFloat(ptr + 0, x);
                    MemoryUtil.memPutFloat(ptr + 4, y);
                    MemoryUtil.memPutFloat(ptr + 8, z);
                    instanceBufferPosition += INSTANCE_STRUCT_STRIDE;
                    batchInstanceCount++;
                
                    largestVertexIndex = Math.max(largestVertexIndex, geometry.segment.getLength());
                }
            
                if (batchCommandCount == 0) {
                    continue;
                }
            
                int commandSubsectionLength = batchCommandCount * COMMAND_STRUCT_STRIDE;
                long commandSubsectionStart = commandBufferSection.getDeviceOffset()
                                              + commandBufferPosition - commandSubsectionLength;
                commandBufferPosition = MathUtil.align(
                        commandBufferPosition,
                        this.commandBuffer.getAlignment()
                );
            
                int instanceSubsectionLength = batchInstanceCount * INSTANCE_STRUCT_STRIDE;
                long instanceSubsectionStart = instanceBufferSection.getDeviceOffset()
                                               + instanceBufferPosition - instanceSubsectionLength;
                instanceBufferPosition = MathUtil.align(
                        instanceBufferPosition,
                        this.uniformBufferInstanceData.getAlignment()
                );
            
                RenderRegion region = regionBucket.getRegion();
            
                renderList.add(new MdiChunkRenderBatch(
                        region.vertexBuffers.getBufferObject(),
                        region.vertexBuffers.getStride(),
                        batchInstanceCount,
                        batchCommandCount,
                        instanceSubsectionStart,
                        commandSubsectionStart
                ));
            
            }
        
            if (!renderList.isEmpty()) {
                renderLists[passId] = renderList;
            }
        }
        
        commandBufferSectionView.position(commandBufferPosition);
        instanceBufferSectionView.position(instanceBufferPosition);

        commandBufferSection.flushPartial();
        instanceBufferSection.flushPartial();

        this.indexBuffer.ensureCapacity(largestVertexIndex);

        this.renderLists = renderLists;
    }

    @Override
    public void delete() {
        super.delete();
        this.uniformBufferCameraMatrices.delete();
        this.uniformBufferInstanceData.delete();
        this.uniformBufferFogParameters.delete();
        this.commandBuffer.delete();
        this.indexBuffer.delete();
    }

    protected static class MdiChunkRenderBatch {
        protected final Buffer vertexBuffer;
        protected final int vertexStride;
        protected final int instanceCount;
        protected final int commandCount;
        protected long instanceBufferOffset;
        protected long commandBufferOffset;

        public MdiChunkRenderBatch(
                Buffer vertexBuffer,
                int vertexStride,
                int instanceCount,
                int commandCount,
                long instanceBufferOffset,
                long commandBufferOffset
        ) {
            this.vertexBuffer = vertexBuffer;
            this.vertexStride = vertexStride;
            this.instanceCount = instanceCount;
            this.commandCount = commandCount;
            this.instanceBufferOffset = instanceBufferOffset;
            this.commandBufferOffset = commandBufferOffset;
        }

        public Buffer getVertexBuffer() {
            return this.vertexBuffer;
        }

        public int getVertexStride() {
            return this.vertexStride;
        }

        public int getInstanceCount() {
            return this.instanceCount;
        }

        public int getCommandCount() {
            return this.commandCount;
        }

        public long getInstanceBufferOffset() {
            return this.instanceBufferOffset;
        }

        public long getCommandBufferOffset() {
            return this.commandBufferOffset;
        }
    }

    protected static int commandBufferPassSize(int alignment, SortedChunkLists list) {
        int size = 0;

        for (SortedChunkLists.RegionBucket regionBucket : list.unsortedRegionBuckets()) {
            size += MathUtil.align((regionBucket.sectionCount() * ChunkMeshFace.COUNT) * COMMAND_STRUCT_STRIDE, alignment);
        }

        return size;
    }

    protected static int instanceBufferPassSize(int alignment, SortedChunkLists list) {
        int size = 0;

        for (SortedChunkLists.RegionBucket regionBucket : list.unsortedRegionBuckets()) {
            size += MathUtil.align(regionBucket.sectionCount() * INSTANCE_STRUCT_STRIDE, alignment);
        }

        return size;
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

        this.device.usePipeline(pipeline, (cmd, programInterface, pipelineState) -> {
            this.setupTextures(renderPass, pipelineState);
            this.setupUniforms(matrices, programInterface, pipelineState, frameIndex);

            cmd.bindCommandBuffer(this.commandBuffer.getBufferObject());
            cmd.bindElementBuffer(this.indexBuffer.getBuffer());

            for (var batch : renderList) {
                pipelineState.bindBufferBlock(
                        programInterface.uniformInstanceData,
                        this.uniformBufferInstanceData.getBufferObject(),
                        batch.getInstanceBufferOffset(),
                        INSTANCE_DATA_SIZE // the spec requires that the entire part of the UBO is filled completely, so lets just make the range the right size
                );

                cmd.bindVertexBuffer(BufferTarget.VERTICES, batch.getVertexBuffer(), 0, batch.getVertexStride());

                cmd.multiDrawElementsIndirect(
                        PrimitiveType.TRIANGLES,
                        ElementFormat.UNSIGNED_INT,
                        batch.getCommandBufferOffset(),
                        batch.getCommandCount(),
                        0
                );
            }
        });
    }

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

    protected static ShaderConstants getShaderConstants(ChunkRenderPass pass, TerrainVertexType vertexType) {
        var constants = ShaderConstants.builder();

        if (pass.isCutout()) {
            constants.add("ALPHA_CUTOFF", String.valueOf(pass.getAlphaCutoff()));
        }

        if (!MathHelper.approximatelyEquals(vertexType.getVertexRange(), 1.0f)) {
            constants.add("VERT_SCALE", String.valueOf(vertexType.getVertexRange()));
        }

        return constants.build();
    }
}
