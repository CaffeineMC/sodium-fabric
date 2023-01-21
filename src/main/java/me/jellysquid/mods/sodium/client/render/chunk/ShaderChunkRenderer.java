package me.jellysquid.mods.sodium.client.render.chunk;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import me.jellysquid.mods.sodium.client.gl.attribute.GlVertexFormat;
import me.jellysquid.mods.sodium.client.gl.shader.*;
import me.jellysquid.mods.sodium.client.gl.device.RenderDevice;
import me.jellysquid.mods.sodium.client.render.vertex.type.ChunkVertexType;
import me.jellysquid.mods.sodium.client.render.chunk.format.ChunkMeshAttribute;
import me.jellysquid.mods.sodium.client.render.chunk.passes.BlockRenderPass;
import me.jellysquid.mods.sodium.client.render.chunk.shader.ChunkFogMode;
import me.jellysquid.mods.sodium.client.render.chunk.shader.ChunkShaderInterface;
import me.jellysquid.mods.sodium.client.render.chunk.shader.ChunkShaderBindingPoints;
import me.jellysquid.mods.sodium.client.render.chunk.shader.ChunkShaderOptions;
import net.minecraft.util.Identifier;

import java.util.Map;

public abstract class ShaderChunkRenderer implements ChunkRenderer {
    private final Map<ChunkShaderOptions, GlProgram<ChunkShaderInterface>> programs = new Object2ObjectOpenHashMap<>();

    protected final ChunkVertexType vertexType;
    protected final GlVertexFormat<ChunkMeshAttribute> vertexFormat;

    protected final RenderDevice device;

    protected GlProgram<ChunkShaderInterface> activeProgram;

    public ShaderChunkRenderer(RenderDevice device, ChunkVertexType vertexType) {
        this.device = device;
        this.vertexType = vertexType;
        this.vertexFormat = vertexType.getVertexFormat();
    }

    protected GlProgram<ChunkShaderInterface> compileProgram(ChunkShaderOptions options) {
        GlProgram<ChunkShaderInterface> program = this.programs.get(options);

        if (program == null) {
            this.programs.put(options, program = this.createShader("blocks/block_layer_opaque", options));
        }

        return program;
    }

    private GlProgram<ChunkShaderInterface> createShader(String path, ChunkShaderOptions options) {
        ShaderConstants constants = options.constants();

        GlShader vertShader = ShaderLoader.loadShader(ShaderType.VERTEX,
                new Identifier("sodium", path + ".vsh"), constants);
        
        GlShader fragShader = ShaderLoader.loadShader(ShaderType.FRAGMENT,
                new Identifier("sodium", path + ".fsh"), constants);

        try {
            return GlProgram.builder(new Identifier("sodium", "chunk_shader"))
                    .attachShader(vertShader)
                    .attachShader(fragShader)
                    .bindAttribute("a_PosId", ChunkShaderBindingPoints.ATTRIBUTE_POSITION_ID)
                    .bindAttribute("a_Color", ChunkShaderBindingPoints.ATTRIBUTE_COLOR)
                    .bindAttribute("a_TexCoord", ChunkShaderBindingPoints.ATTRIBUTE_BLOCK_TEXTURE)
                    .bindAttribute("a_LightCoord", ChunkShaderBindingPoints.ATTRIBUTE_LIGHT_TEXTURE)
                    .bindFragmentData("fragColor", ChunkShaderBindingPoints.FRAG_COLOR)
                    .link((shader) -> new ChunkShaderInterface(shader, options));
        } finally {
            vertShader.delete();
            fragShader.delete();
        }
    }

    protected void begin(BlockRenderPass pass) {
        ChunkShaderOptions options = new ChunkShaderOptions(ChunkFogMode.SMOOTH, pass, this.vertexType);

        this.activeProgram = this.compileProgram(options);
        this.activeProgram.bind();
        this.activeProgram.getInterface()
                .setup(this.vertexType);
    }

    protected void end() {
        this.activeProgram.unbind();
        this.activeProgram = null;
    }

    @Override
    public void delete() {
        this.programs.values()
                .forEach(GlProgram::delete);
        this.programs.clear();
    }

    @Override
    public ChunkVertexType getVertexType() {
        return this.vertexType;
    }
}
