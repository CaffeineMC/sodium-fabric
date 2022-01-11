package me.jellysquid.mods.sodium.render.chunk.draw;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import me.jellysquid.mods.sodium.opengl.array.VertexArrayDescription;
import me.jellysquid.mods.sodium.opengl.array.VertexArrayResourceBinding;
import me.jellysquid.mods.sodium.opengl.attribute.VertexAttributeBinding;
import me.jellysquid.mods.sodium.opengl.attribute.VertexFormat;
import me.jellysquid.mods.sodium.opengl.device.RenderDevice;
import me.jellysquid.mods.sodium.opengl.pipeline.Pipeline;
import me.jellysquid.mods.sodium.opengl.sampler.Sampler;
import me.jellysquid.mods.sodium.opengl.shader.Program;
import me.jellysquid.mods.sodium.opengl.shader.ShaderDescription;
import me.jellysquid.mods.sodium.opengl.shader.ShaderType;
import me.jellysquid.mods.sodium.render.chunk.passes.ChunkRenderPass;
import me.jellysquid.mods.sodium.render.chunk.shader.ChunkShaderBindingPoints;
import me.jellysquid.mods.sodium.render.chunk.shader.ChunkShaderInterface;
import me.jellysquid.mods.sodium.render.shader.ShaderConstants;
import me.jellysquid.mods.sodium.render.shader.ShaderLoader;
import me.jellysquid.mods.sodium.render.shader.ShaderParser;
import me.jellysquid.mods.sodium.render.terrain.format.TerrainMeshAttribute;
import me.jellysquid.mods.sodium.render.terrain.format.TerrainVertexType;
import net.minecraft.util.Identifier;
import org.lwjgl.opengl.GL33C;

import java.util.List;
import java.util.Map;

public abstract class ShaderChunkRenderer implements ChunkRenderer {
    private final Map<ChunkRenderPass, Pipeline<ChunkShaderInterface, BufferTarget>> pipelines = new Object2ObjectOpenHashMap<>();
    private final Map<ChunkRenderPass, Program<ChunkShaderInterface>> programs = new Object2ObjectOpenHashMap<>();

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

    protected Pipeline<ChunkShaderInterface, BufferTarget> getPipeline(ChunkRenderPass pass) {
        return this.pipelines.computeIfAbsent(pass, this::createPipeline);
    }

    private Pipeline<ChunkShaderInterface, BufferTarget> createPipeline(ChunkRenderPass pass) {
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

        return this.device.createPipeline(pass.renderState(), program, vertexArray);
    }

    private Program<ChunkShaderInterface> getProgram(ChunkRenderPass pass) {
        return this.programs.computeIfAbsent(pass, this::createProgram);
    }

    private Program<ChunkShaderInterface> createProgram(ChunkRenderPass pass) {
        var constants = getShaderConstants(pass, this.vertexType);

        var vertShader = ShaderParser.parseShader(ShaderLoader.MINECRAFT_ASSETS, new Identifier("sodium", "blocks/block_layer_opaque.vsh"), constants);
        var fragShader = ShaderParser.parseShader(ShaderLoader.MINECRAFT_ASSETS, new Identifier("sodium", "blocks/block_layer_opaque.fsh"), constants);

        var desc = ShaderDescription.builder()
                .addShaderSource(ShaderType.VERTEX, vertShader)
                .addShaderSource(ShaderType.FRAGMENT, fragShader)
                .addAttributeBinding("a_Position", ChunkShaderBindingPoints.ATTRIBUTE_POSITION)
                .addAttributeBinding("a_Color", ChunkShaderBindingPoints.ATTRIBUTE_COLOR)
                .addAttributeBinding("a_TexCoord", ChunkShaderBindingPoints.ATTRIBUTE_BLOCK_TEXTURE)
                .addAttributeBinding("a_LightCoord", ChunkShaderBindingPoints.ATTRIBUTE_LIGHT_TEXTURE)
                .addFragmentBinding("fragColor", ChunkShaderBindingPoints.FRAG_COLOR)
                .build();

        return this.device.createProgram(desc, ChunkShaderInterface::new);
    }

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

    private static ShaderConstants getShaderConstants(ChunkRenderPass pass, TerrainVertexType vertexType) {
        var constants = ShaderConstants.builder();

        if (pass.isCutout()) {
            constants.add("ALPHA_CUTOFF", String.valueOf(pass.alphaCutoff()));
        }

        constants.add("USE_VERTEX_COMPRESSION"); // TODO: allow compact vertex format to be disabled
        constants.add("VERT_POS_SCALE", String.valueOf(vertexType.getPositionScale()));
        constants.add("VERT_POS_OFFSET", String.valueOf(vertexType.getPositionOffset()));
        constants.add("VERT_TEX_SCALE", String.valueOf(vertexType.getTextureScale()));

        return constants.build();
    }
}
