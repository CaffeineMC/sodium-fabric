package me.jellysquid.mods.sodium.render.chunk.draw;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import me.jellysquid.mods.sodium.opengl.attribute.VertexFormat;
import me.jellysquid.mods.sodium.opengl.device.RenderDevice;
import me.jellysquid.mods.sodium.opengl.sampler.Sampler;
import me.jellysquid.mods.sodium.opengl.shader.Program;
import me.jellysquid.mods.sodium.opengl.shader.ShaderDescription;
import me.jellysquid.mods.sodium.opengl.shader.ShaderType;
import me.jellysquid.mods.sodium.render.chunk.shader.ChunkShaderBindingPoints;
import me.jellysquid.mods.sodium.render.chunk.shader.ChunkShaderInterface;
import me.jellysquid.mods.sodium.render.chunk.shader.ChunkShaderOptions;
import me.jellysquid.mods.sodium.render.shader.ShaderConstants;
import me.jellysquid.mods.sodium.render.shader.ShaderLoader;
import me.jellysquid.mods.sodium.render.shader.ShaderParser;
import me.jellysquid.mods.sodium.render.terrain.format.TerrainMeshAttribute;
import me.jellysquid.mods.sodium.render.terrain.format.TerrainVertexType;
import net.minecraft.util.Identifier;
import org.lwjgl.opengl.GL33C;

import java.util.Map;

public abstract class ShaderChunkRenderer implements ChunkRenderer {
    private final Map<ChunkShaderOptions, Program<ChunkShaderInterface>> programs = new Object2ObjectOpenHashMap<>();

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

        // TODO: delete these objects after use
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

    protected Program<ChunkShaderInterface> compileProgram(ChunkShaderOptions options) {
        Program<ChunkShaderInterface> program = this.programs.get(options);

        if (program == null) {
            this.programs.put(options, program = this.createShader("blocks/block_layer_opaque", options));
        }

        return program;
    }

    private Program<ChunkShaderInterface> createShader(String path, ChunkShaderOptions options) {
        ShaderConstants constants = options.constants();

        var vertShader = ShaderParser.parseShader(ShaderLoader.MINECRAFT_ASSETS, new Identifier("sodium", path + ".vsh"), constants);
        var fragShader = ShaderParser.parseShader(ShaderLoader.MINECRAFT_ASSETS, new Identifier("sodium", path + ".fsh"), constants);

        var desc = ShaderDescription.builder()
                .addShaderSource(ShaderType.VERTEX, vertShader)
                .addShaderSource(ShaderType.FRAGMENT, fragShader)
                .addAttributeBinding("a_PosId", ChunkShaderBindingPoints.ATTRIBUTE_POSITION_ID)
                .addAttributeBinding("a_Color", ChunkShaderBindingPoints.ATTRIBUTE_COLOR)
                .addAttributeBinding("a_TexCoord", ChunkShaderBindingPoints.ATTRIBUTE_BLOCK_TEXTURE)
                .addAttributeBinding("a_LightCoord", ChunkShaderBindingPoints.ATTRIBUTE_LIGHT_TEXTURE)
                .addFragmentBinding("fragColor", ChunkShaderBindingPoints.FRAG_COLOR)
                .build();

        return this.device.createProgram(desc, ChunkShaderInterface::new);
    }

    @Override
    public void delete() {
        for (var program : this.programs.values()) {
            this.device.deleteProgram(program);
        }

        this.programs.clear();
    }

    @Override
    public TerrainVertexType getVertexType() {
        return this.vertexType;
    }
}
