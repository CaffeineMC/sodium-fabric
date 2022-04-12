package net.caffeinemc.sodium.render.chunk.draw;

import com.mojang.blaze3d.systems.RenderSystem;
import net.caffeinemc.gfx.api.array.VertexArrayDescription;
import net.caffeinemc.gfx.api.array.VertexArrayResourceBinding;
import net.caffeinemc.gfx.api.array.attribute.VertexAttributeBinding;
import net.caffeinemc.gfx.api.device.RenderDevice;
import net.caffeinemc.gfx.api.pipeline.Pipeline;
import net.caffeinemc.gfx.api.pipeline.PipelineState;
import net.caffeinemc.gfx.api.shader.*;
import net.caffeinemc.gfx.api.types.ElementFormat;
import net.caffeinemc.gfx.api.types.PrimitiveType;
import net.caffeinemc.sodium.SodiumClientMod;
import net.caffeinemc.sodium.render.buffer.StreamingBuffer;
import net.caffeinemc.sodium.render.chunk.passes.ChunkRenderPass;
import net.caffeinemc.sodium.render.chunk.shader.ChunkShaderBindingPoints;
import net.caffeinemc.sodium.render.chunk.shader.ChunkShaderInterface;
import net.caffeinemc.sodium.render.sequence.SequenceIndexBuffer;
import net.caffeinemc.sodium.render.shader.ShaderConstants;
import net.caffeinemc.sodium.render.shader.ShaderLoader;
import net.caffeinemc.sodium.render.terrain.format.TerrainMeshAttribute;
import net.caffeinemc.sodium.render.terrain.format.TerrainVertexType;
import net.caffeinemc.sodium.util.TextureUtil;
import net.minecraft.util.Identifier;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DefaultChunkRenderer extends AbstractChunkRenderer {
    private final Pipeline<ChunkShaderInterface, BufferTarget> pipeline;
    private final Program<ChunkShaderInterface> program;

    private final StreamingBuffer bufferCameraMatrices;

    private final StreamingBuffer bufferFogParameters;

    private final SequenceIndexBuffer indexBuffer;

    public DefaultChunkRenderer(RenderDevice device, SequenceIndexBuffer indexBuffer, TerrainVertexType vertexType, ChunkRenderPass pass) {
        super(device, vertexType);

        var maxInFlightFrames = SodiumClientMod.options().advanced.cpuRenderAheadLimit + 1;

        this.bufferCameraMatrices = new StreamingBuffer(device, 192, maxInFlightFrames);
        this.bufferFogParameters = new StreamingBuffer(device, 32, maxInFlightFrames);

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

        var shaderName = pass.shaderName();
        
        var vertShader = ShaderLoader.MINECRAFT_ASSETS.getShaderSource(new Identifier("sodium", "terrain/%s.vert.spv".formatted(shaderName)));
        var fragShader = ShaderLoader.MINECRAFT_ASSETS.getShaderSource(new Identifier("sodium", "terrain/%s.frag.spv".formatted(shaderName)));

        var desc = ProgramDescription.builder()
                .addShaderBinary(ShaderType.VERTEX, new ShaderDescription(vertShader, Collections.emptyList()))
                .addShaderBinary(ShaderType.FRAGMENT, new ShaderDescription(fragShader, getFragmentShaderConstants(pass)))
                .build();

        this.program = this.device.createProgram(desc, ChunkShaderInterface::new);
        this.pipeline = this.device.createPipeline(pass.pipelineDescription(), this.program, vertexArray);
    }

    @Override
    public void render(ChunkPrep.PreparedRenderList lists, ChunkRenderPass renderPass, ChunkRenderMatrices matrices, int frameIndex) {
        this.indexBuffer.ensureCapacity(lists.largestVertexIndex());

        this.device.usePipeline(this.pipeline, (cmd, programInterface, pipelineState) -> {
            this.setupTextures(renderPass, pipelineState);
            this.setupUniforms(matrices, programInterface, pipelineState, frameIndex);

            cmd.bindCommandBuffer(lists.commandBuffer());
            cmd.bindElementBuffer(this.indexBuffer.getBuffer());

            for (var batch : lists.batches()) {
                pipelineState.bindUniformBlock(programInterface.uniformInstanceData, lists.instanceBuffer(),
                        batch.instanceData().offset(), batch.instanceData().length());

                cmd.bindVertexBuffer(BufferTarget.VERTICES, batch.vertexBuffer(), 0, batch.vertexStride());

                cmd.multiDrawElementsIndirect(PrimitiveType.TRIANGLES, ElementFormat.UNSIGNED_INT, batch.commandData().offset(), batch.commandCount());
            }
        });
    }

    private void setupTextures(ChunkRenderPass pass, PipelineState pipelineState) {
        pipelineState.bindTexture(0, TextureUtil.getBlockAtlasTexture(), pass.mipped() ? this.blockTextureMippedSampler : this.blockTextureSampler);
        pipelineState.bindTexture(1, TextureUtil.getLightTexture(), this.lightTextureSampler);
    }

    private void setupUniforms(ChunkRenderMatrices renderMatrices, ChunkShaderInterface programInterface, PipelineState state, int frameIndex) {
        var matrices = this.bufferCameraMatrices.slice(frameIndex);
        var matricesBuf = matrices.view();

        renderMatrices.projection()
                .get(0, matricesBuf);
        renderMatrices.modelView()
                .get(64, matricesBuf);

        var mvpMatrix = new Matrix4f();
        mvpMatrix.set(renderMatrices.projection());
        mvpMatrix.mul(renderMatrices.modelView());
        mvpMatrix
                .get(128, matricesBuf);

        state.bindUniformBlock(programInterface.uniformCameraMatrices, matrices.buffer(), matrices.offset(), matrices.length());

        var fogParams = this.bufferFogParameters.slice(frameIndex);
        var fogParamsBuf = fogParams.view();

        var paramFogColor = RenderSystem.getShaderFogColor();
        fogParamsBuf.putFloat(0, paramFogColor[0]);
        fogParamsBuf.putFloat(4, paramFogColor[1]);
        fogParamsBuf.putFloat(8, paramFogColor[2]);
        fogParamsBuf.putFloat(12, paramFogColor[3]);
        fogParamsBuf.putFloat(16, RenderSystem.getShaderFogStart());
        fogParamsBuf.putFloat(20, RenderSystem.getShaderFogEnd());
        fogParamsBuf.putInt(24, RenderSystem.getShaderFogShape().getId());

        state.bindUniformBlock(programInterface.uniformFogParameters, fogParams.buffer(), fogParams.offset(), fogParams.length());
    }

    private static List<SpecializationConstant> getFragmentShaderConstants(ChunkRenderPass pass) {
        var constants = new ArrayList<SpecializationConstant>();

        if (pass.isCutout()) {
            constants.add(SpecializationConstant.ofFloat(0, pass.alphaCutoff()));
        }

        return Collections.unmodifiableList(constants);
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
