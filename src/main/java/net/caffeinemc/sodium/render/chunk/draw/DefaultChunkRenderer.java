package net.caffeinemc.sodium.render.chunk.draw;

import com.mojang.blaze3d.systems.RenderSystem;
import net.caffeinemc.gfx.api.buffer.BufferMapFlags;
import net.caffeinemc.gfx.api.buffer.BufferStorageFlags;
import net.caffeinemc.gfx.api.buffer.MappedBuffer;
import net.caffeinemc.gfx.api.device.RenderDevice;
import net.caffeinemc.gfx.api.pipeline.PipelineState;
import net.caffeinemc.gfx.api.shader.Program;
import net.caffeinemc.gfx.api.shader.ShaderDescription;
import net.caffeinemc.gfx.api.shader.ShaderType;
import net.caffeinemc.gfx.api.types.ElementFormat;
import net.caffeinemc.gfx.api.types.PrimitiveType;
import net.caffeinemc.sodium.render.chunk.passes.ChunkRenderPass;
import net.caffeinemc.sodium.render.chunk.shader.ChunkShaderInterface;
import net.caffeinemc.sodium.render.sequence.SequenceBuilder;
import net.caffeinemc.sodium.render.sequence.SequenceIndexBuffer;
import net.caffeinemc.sodium.render.shader.ShaderConstants;
import net.caffeinemc.sodium.render.shader.ShaderLoader;
import net.caffeinemc.sodium.render.shader.ShaderParser;
import net.caffeinemc.sodium.render.terrain.format.TerrainVertexType;
import net.caffeinemc.sodium.util.TextureUtil;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import org.joml.Matrix4f;

import java.util.EnumSet;

public class DefaultChunkRenderer extends ShaderChunkRenderer<ChunkShaderInterface> {
    private final MappedBuffer bufferCameraMatrices;
    private final MappedBuffer bufferFogParameters;

    private final SequenceIndexBuffer sequenceIndexBuffer;

    public DefaultChunkRenderer(RenderDevice device, TerrainVertexType vertexType) {
        super(device, vertexType);

        var storageFlags = EnumSet.of(BufferStorageFlags.WRITABLE, BufferStorageFlags.COHERENT, BufferStorageFlags.PERSISTENT);
        var mapFlags = EnumSet.of(BufferMapFlags.WRITE, BufferMapFlags.COHERENT, BufferMapFlags.PERSISTENT);

        this.bufferCameraMatrices = device.createMappedBuffer(192, storageFlags, mapFlags);
        this.bufferFogParameters = device.createMappedBuffer(24, storageFlags, mapFlags);

        this.sequenceIndexBuffer = new SequenceIndexBuffer(device, SequenceBuilder.QUADS);
    }

    @Override
    public void render(ChunkPrep.PreparedRenderList lists, ChunkRenderPass renderPass, ChunkRenderMatrices matrices) {
        this.sequenceIndexBuffer.ensureCapacity(lists.largestVertexIndex());

        this.device.usePipeline(this.getPipeline(renderPass), (cmd, programInterface, pipelineState) -> {
            this.setupTextures(renderPass, pipelineState);
            this.setupUniforms(matrices, programInterface, pipelineState);

            for (var batch : lists.batches()) {
                pipelineState.bindUniformBlock(programInterface.uniformInstanceData, lists.instanceBuffer(),
                        batch.instanceData().offset(), batch.instanceData().length());

                cmd.bindVertexBuffer(BufferTarget.VERTICES, batch.vertexBuffer(), 0, this.vertexFormat.stride());
                cmd.bindElementBuffer(this.sequenceIndexBuffer.getBuffer());

                cmd.multiDrawElementsIndirect(lists.commandBuffer(), batch.commandData().offset(), batch.commandCount(),
                        ElementFormat.UNSIGNED_INT, PrimitiveType.TRIANGLES);
            }
        });
    }

    private void setupTextures(ChunkRenderPass pass, PipelineState pipelineState) {
        pipelineState.bindTexture(0, TextureUtil.getBlockAtlasTexture(), pass.mipped() ? this.blockTextureMippedSampler : this.blockTextureSampler);
        pipelineState.bindTexture(1, TextureUtil.getLightTexture(), this.lightTextureSampler);
    }

    private void setupUniforms(ChunkRenderMatrices matrices, ChunkShaderInterface programInterface, PipelineState state) {
        var bufMatrices = this.bufferCameraMatrices.view();

        matrices.projection()
                .get(0, bufMatrices);
        matrices.modelView()
                .get(64, bufMatrices);

        var mvpMatrix = new Matrix4f();
        mvpMatrix.set(matrices.projection());
        mvpMatrix.mul(matrices.modelView());
        mvpMatrix
                .get(128, bufMatrices);

        this.bufferCameraMatrices.flush();

        var bufFogParameters = this.bufferFogParameters.view();
        var paramFogColor = RenderSystem.getShaderFogColor();
        bufFogParameters.putFloat(0, paramFogColor[0]);
        bufFogParameters.putFloat(4, paramFogColor[1]);
        bufFogParameters.putFloat(8, paramFogColor[2]);
        bufFogParameters.putFloat(12, paramFogColor[3]);
        bufFogParameters.putFloat(16, RenderSystem.getShaderFogStart());
        bufFogParameters.putFloat(20, RenderSystem.getShaderFogEnd());

        this.bufferFogParameters.flush();

        state.bindUniformBlock(programInterface.uniformFogParameters, this.bufferFogParameters);
        state.bindUniformBlock(programInterface.uniformCameraMatrices, this.bufferCameraMatrices);
    }

    @Override
    protected Program<ChunkShaderInterface> createProgram(ChunkRenderPass pass) {
        var constants = getShaderConstants(pass, this.vertexType);

        var vertShader = ShaderParser.parseShader(ShaderLoader.MINECRAFT_ASSETS, new Identifier("sodium", "terrain/terrain_opaque.vert"), constants);
        var fragShader = ShaderParser.parseShader(ShaderLoader.MINECRAFT_ASSETS, new Identifier("sodium", "terrain/terrain_opaque.frag"), constants);

        var desc = ShaderDescription.builder()
                .addShaderSource(ShaderType.VERTEX, vertShader)
                .addShaderSource(ShaderType.FRAGMENT, fragShader)
                .build();

        return this.device.createProgram(desc, ChunkShaderInterface::new);
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

        this.device.deleteBuffer(this.bufferFogParameters);
        this.device.deleteBuffer(this.bufferCameraMatrices);
    }
}
