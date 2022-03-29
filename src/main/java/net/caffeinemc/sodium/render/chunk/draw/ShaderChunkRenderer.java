package net.caffeinemc.sodium.render.chunk.draw;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.caffeinemc.gfx.api.array.VertexArrayDescription;
import net.caffeinemc.gfx.api.array.VertexArrayResourceBinding;
import net.caffeinemc.gfx.api.array.attribute.VertexAttributeBinding;
import net.caffeinemc.gfx.api.array.attribute.VertexFormat;
import net.caffeinemc.gfx.api.device.RenderDevice;
import net.caffeinemc.gfx.api.pipeline.Pipeline;
import net.caffeinemc.gfx.api.texture.Sampler;
import net.caffeinemc.gfx.api.shader.Program;
import net.caffeinemc.gfx.api.shader.ShaderDescription;
import net.caffeinemc.gfx.api.shader.ShaderType;
import net.caffeinemc.sodium.render.chunk.passes.ChunkRenderPass;
import net.caffeinemc.sodium.render.chunk.shader.ChunkShaderBindingPoints;
import net.caffeinemc.sodium.render.chunk.shader.ChunkShaderInterface;
import net.caffeinemc.sodium.render.shader.ShaderConstants;
import net.caffeinemc.sodium.render.shader.ShaderLoader;
import net.caffeinemc.sodium.render.shader.ShaderParser;
import net.caffeinemc.sodium.render.terrain.format.TerrainMeshAttribute;
import net.caffeinemc.sodium.render.terrain.format.TerrainVertexType;
import net.minecraft.util.Identifier;
import org.lwjgl.opengl.GL33C;

import java.util.List;
import java.util.Map;

public abstract class ShaderChunkRenderer<T> implements ChunkRenderer {
    private final Map<ChunkRenderPass, Pipeline<T, BufferTarget>> pipelines = new Object2ObjectOpenHashMap<>();
    private final Map<ChunkRenderPass, Program<T>> programs = new Object2ObjectOpenHashMap<>();

    protected final TerrainVertexType vertexType;
    protected final VertexFormat<TerrainMeshAttribute> vertexFormat;

    protected final RenderDevice device;

    protected final Sampler blockTextureSampler;
    protected final Sampler blockTextureMippedSampler;

    protected final Sampler lightTextureSampler;

    public ShaderChunkRenderer(RenderDevice device, TerrainVertexType vertexType) {
        this.device = device;
        this.vertexType = vertexType;
        this.vertexFormat = vertexType.getCustomVertexFormat();

        this.blockTextureSampler = device.createSampler();
        this.blockTextureSampler.setParameter(GL33C.GL_TEXTURE_MIN_FILTER, GL33C.GL_NEAREST);
        this.blockTextureSampler.setParameter(GL33C.GL_TEXTURE_MAG_FILTER, GL33C.GL_NEAREST);

        this.blockTextureMippedSampler = device.createSampler();
        this.blockTextureMippedSampler.setParameter(GL33C.GL_TEXTURE_MIN_FILTER, GL33C.GL_NEAREST_MIPMAP_LINEAR);
        this.blockTextureMippedSampler.setParameter(GL33C.GL_TEXTURE_MAG_FILTER, GL33C.GL_NEAREST);

        this.lightTextureSampler = device.createSampler();
        this.lightTextureSampler.setParameter(GL33C.GL_TEXTURE_MIN_FILTER, GL33C.GL_LINEAR);
        this.lightTextureSampler.setParameter(GL33C.GL_TEXTURE_MAG_FILTER, GL33C.GL_LINEAR);
        this.lightTextureSampler.setParameter(GL33C.GL_TEXTURE_WRAP_S, GL33C.GL_CLAMP_TO_EDGE);
        this.lightTextureSampler.setParameter(GL33C.GL_TEXTURE_WRAP_T, GL33C.GL_CLAMP_TO_EDGE);
    }

    protected Pipeline<T, BufferTarget> getPipeline(ChunkRenderPass pass) {
        return this.pipelines.computeIfAbsent(pass, this::createPipeline);
    }

    private Pipeline<T, BufferTarget> createPipeline(ChunkRenderPass pass) {
        var program = this.getProgram(pass);

        var vertexArray = new VertexArrayDescription<>(BufferTarget.values(), List.of(
                new VertexArrayResourceBinding<>(BufferTarget.VERTICES, new VertexAttributeBinding[] {
                        new VertexAttributeBinding(ChunkShaderBindingPoints.ATTRIBUTE_POSITION,
                                this.vertexFormat.getAttribute(TerrainMeshAttribute.POSITION)),
                        new VertexAttributeBinding(ChunkShaderBindingPoints.ATTRIBUTE_COLOR,
                                this.vertexFormat.getAttribute(TerrainMeshAttribute.COLOR)),
                        new VertexAttributeBinding(ChunkShaderBindingPoints.ATTRIBUTE_BLOCK_TEXTURE,
                                this.vertexFormat.getAttribute(TerrainMeshAttribute.BLOCK_TEXTURE)),
                        new VertexAttributeBinding(ChunkShaderBindingPoints.ATTRIBUTE_LIGHT_TEXTURE,
                                this.vertexFormat.getAttribute(TerrainMeshAttribute.LIGHT_TEXTURE))
                })
        ));

        return this.device.createPipeline(pass.pipelineDescription(), program, vertexArray);
    }

    private Program<T> getProgram(ChunkRenderPass pass) {
        return this.programs.computeIfAbsent(pass, this::createProgram);
    }

    protected abstract Program<T> createProgram(ChunkRenderPass pass);

    @Override
    public void delete() {
        for (var pipeline : this.pipelines.values()) {
            this.device.deletePipeline(pipeline);
        }

        this.pipelines.clear();

        for (var program : this.programs.values()) {
            this.device.deleteProgram(program);
        }

        this.programs.clear();

        this.device.deleteSampler(this.blockTextureSampler);
        this.device.deleteSampler(this.blockTextureMippedSampler);
        this.device.deleteSampler(this.lightTextureSampler);
    }

    public enum BufferTarget {
        VERTICES
    }
}
