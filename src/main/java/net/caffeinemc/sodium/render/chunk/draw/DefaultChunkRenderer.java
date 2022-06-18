package net.caffeinemc.sodium.render.chunk.draw;

import com.mojang.blaze3d.systems.RenderSystem;

import java.util.EnumSet;

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
import net.caffeinemc.sodium.SodiumClientMod;
import net.caffeinemc.sodium.render.buffer.streaming.SectionedStreamingBuffer;
import net.caffeinemc.sodium.render.buffer.streaming.StreamingBuffer;
import net.caffeinemc.sodium.render.chunk.passes.ChunkRenderPass;
import net.caffeinemc.sodium.render.chunk.region.RenderRegion;
import net.caffeinemc.sodium.render.chunk.shader.ChunkShaderBindingPoints;
import net.caffeinemc.sodium.render.chunk.shader.ChunkShaderInterface;
import net.caffeinemc.sodium.render.sequence.SequenceIndexBuffer;
import net.caffeinemc.sodium.render.shader.ShaderConstants;
import net.caffeinemc.sodium.render.shader.ShaderLoader;
import net.caffeinemc.sodium.render.shader.ShaderParser;
import net.caffeinemc.sodium.render.terrain.format.TerrainMeshAttribute;
import net.caffeinemc.sodium.render.terrain.format.TerrainVertexType;
import net.caffeinemc.sodium.util.TextureUtil;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import org.joml.Matrix4f;

import java.util.List;

public class DefaultChunkRenderer extends AbstractChunkRenderer {
    // TODO: should these be moved?
    public static final int CAMERA_MATRICES_SIZE = 192;
    public static final int FOG_PARAMETERS_SIZE = 32;
    public static final int INSTANCE_DATA_SIZE = RenderRegion.REGION_SIZE * RenderListBuilder.INSTANCE_STRUCT_STRIDE;

    private final Pipeline<ChunkShaderInterface, BufferTarget> pipeline;
    private final Program<ChunkShaderInterface> program;

    private final StreamingBuffer bufferCameraMatrices;
    private final StreamingBuffer bufferInstanceData;
    private final StreamingBuffer bufferFogParameters;

    private final StreamingBuffer commandBuffer;

    private final SequenceIndexBuffer indexBuffer;

    public DefaultChunkRenderer(RenderDevice device, StreamingBuffer instanceBuffer, StreamingBuffer commandBuffer, SequenceIndexBuffer indexBuffer, TerrainVertexType vertexType, ChunkRenderPass pass) {
        super(device, vertexType);

        var maxInFlightFrames = SodiumClientMod.options().advanced.cpuRenderAheadLimit + 1;

        final int alignment = device.properties().uniformBufferOffsetAlignment;
        this.bufferCameraMatrices = new SectionedStreamingBuffer(
                device,
                alignment,
                CAMERA_MATRICES_SIZE,
                maxInFlightFrames,
                EnumSet.of(MappedBufferFlags.EXPLICIT_FLUSH)
        );
        this.bufferFogParameters = new SectionedStreamingBuffer(
                device,
                alignment,
                FOG_PARAMETERS_SIZE,
                maxInFlightFrames,
                EnumSet.of(MappedBufferFlags.EXPLICIT_FLUSH)
        );
        this.bufferInstanceData = instanceBuffer;

        this.commandBuffer = commandBuffer;
        this.indexBuffer = indexBuffer;

        var vertexFormat = vertexType.getCustomVertexFormat();
        var vertexArray = new VertexArrayDescription<>(BufferTarget.values(), List.of(
                new VertexArrayResourceBinding<>(BufferTarget.VERTICES, new VertexAttributeBinding[] {
                        new VertexAttributeBinding(ChunkShaderBindingPoints.ATTRIBUTE_POSITION,
                                vertexFormat.getAttribute(TerrainMeshAttribute.POSITION)),
                        new VertexAttributeBinding(ChunkShaderBindingPoints.ATTRIBUTE_COLOR,
                                vertexFormat.getAttribute(TerrainMeshAttribute.COLOR)),
                        new VertexAttributeBinding(ChunkShaderBindingPoints.ATTRIBUTE_BLOCK_TEXTURE,
                                vertexFormat.getAttribute(TerrainMeshAttribute.BLOCK_TEXTURE)),
                        new VertexAttributeBinding(ChunkShaderBindingPoints.ATTRIBUTE_LIGHT_TEXTURE,
                                vertexFormat.getAttribute(TerrainMeshAttribute.LIGHT_TEXTURE))
                })
        ));

        var constants = getShaderConstants(pass, this.vertexType);

        var vertShader = ShaderParser.parseSodiumShader(ShaderLoader.MINECRAFT_ASSETS, new Identifier("sodium", "terrain/terrain_opaque.vert"), constants);
        var fragShader = ShaderParser.parseSodiumShader(ShaderLoader.MINECRAFT_ASSETS, new Identifier("sodium", "terrain/terrain_opaque.frag"), constants);

        var desc = ShaderDescription.builder()
                .addShaderSource(ShaderType.VERTEX, vertShader)
                .addShaderSource(ShaderType.FRAGMENT, fragShader)
                .build();

        this.program = this.device.createProgram(desc, ChunkShaderInterface::new);
        this.pipeline = this.device.createPipeline(pass.pipelineDescription(), this.program, vertexArray);
    }

    @Override
    public void render(RenderListBuilder.RenderList lists, ChunkRenderPass renderPass, ChunkRenderMatrices matrices, int frameIndex) {
        this.indexBuffer.ensureCapacity(lists.getLargestVertexIndex());

        this.device.usePipeline(this.pipeline, (cmd, programInterface, pipelineState) -> {
            this.setupTextures(renderPass, pipelineState);
            this.setupUniforms(matrices, programInterface, pipelineState, frameIndex);

            cmd.bindCommandBuffer(this.commandBuffer.getBufferObject());
            cmd.bindElementBuffer(this.indexBuffer.getBuffer());

            for (var batch : lists.getBatches()) {
                pipelineState.bindBufferBlock(
                        programInterface.uniformInstanceData,
                        this.bufferInstanceData.getBufferObject(),
                        batch.getInstanceBufferOffset(),
                        INSTANCE_DATA_SIZE // the spec requires that the entire part of the UBO is filled completely, so lets just make the range the right size
                );

                cmd.bindVertexBuffer(
                        BufferTarget.VERTICES,
                        batch.getVertexBuffer(),
                        0,
                        batch.getVertexStride()
                );

                cmd.multiDrawElementsIndirect(
                        PrimitiveType.TRIANGLES,
                        ElementFormat.UNSIGNED_INT,
                        batch.getCommandBufferOffset(),
                        batch.getCommandCount()
                );
            }
        });
    }

    private void setupTextures(ChunkRenderPass pass, PipelineState pipelineState) {
        pipelineState.bindTexture(0, TextureUtil.getBlockAtlasTexture(), pass.mipped() ? this.blockTextureMippedSampler : this.blockTextureSampler);
        pipelineState.bindTexture(1, TextureUtil.getLightTexture(), this.lightTextureSampler);
    }

    private void setupUniforms(ChunkRenderMatrices renderMatrices, ChunkShaderInterface programInterface, PipelineState state, int frameIndex) {
        var matricesSection = this.bufferCameraMatrices.getSection(frameIndex);
        var matricesBuf = matricesSection.getView();

        renderMatrices.projection()
                .get(0, matricesBuf);
        renderMatrices.modelView()
                .get(64, matricesBuf);

        var mvpMatrix = new Matrix4f();
        mvpMatrix.set(renderMatrices.projection());
        mvpMatrix.mul(renderMatrices.modelView());
        mvpMatrix
                .get(128, matricesBuf);

        matricesSection.flushFull();

        state.bindBufferBlock(programInterface.uniformCameraMatrices, this.bufferCameraMatrices.getBufferObject(), matricesSection.getOffset(), matricesSection.getView().capacity());

        var fogParamsSection = this.bufferFogParameters.getSection(frameIndex);
        var fogParamsBuf = fogParamsSection.getView();

        var paramFogColor = RenderSystem.getShaderFogColor();
        fogParamsBuf.putFloat(0, paramFogColor[0]);
        fogParamsBuf.putFloat(4, paramFogColor[1]);
        fogParamsBuf.putFloat(8, paramFogColor[2]);
        fogParamsBuf.putFloat(12, paramFogColor[3]);
        fogParamsBuf.putFloat(16, RenderSystem.getShaderFogStart());
        fogParamsBuf.putFloat(20, RenderSystem.getShaderFogEnd());
        fogParamsBuf.putInt(24, RenderSystem.getShaderFogShape().getId());

        fogParamsSection.flushFull();

        state.bindBufferBlock(programInterface.uniformFogParameters, this.bufferFogParameters.getBufferObject(), fogParamsSection.getOffset(), fogParamsSection.getView().capacity());
    }

    private static ShaderConstants getShaderConstants(ChunkRenderPass pass, TerrainVertexType vertexType) {
        var constants = ShaderConstants.builder();

        if (pass.isCutout()) {
            constants.add("ALPHA_CUTOFF", String.valueOf(pass.alphaCutoff()));
        }

        if (!MathHelper.approximatelyEquals(vertexType.getVertexRange(), 1.0f)) {
            constants.add("VERT_SCALE", String.valueOf(vertexType.getVertexRange()));
        }

        return constants.build();
    }

    @Override
    public void delete() {
        super.delete();

        this.device.deletePipeline(this.pipeline);
        this.device.deleteProgram(this.program);

        this.bufferFogParameters.delete();
        this.bufferCameraMatrices.delete();
    }

    public enum BufferTarget {
        VERTICES
    }
}
