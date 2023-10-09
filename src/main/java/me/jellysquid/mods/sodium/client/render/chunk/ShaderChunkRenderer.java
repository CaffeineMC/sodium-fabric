package me.jellysquid.mods.sodium.client.render.chunk;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import me.jellysquid.mods.sodium.client.gl.attribute.GlVertexFormat;
import me.jellysquid.mods.sodium.client.gl.device.CommandList;
import me.jellysquid.mods.sodium.client.gl.device.RenderDevice;
import me.jellysquid.mods.sodium.client.gl.shader.*;
import me.jellysquid.mods.sodium.client.render.chunk.terrain.TerrainRenderPass;
import me.jellysquid.mods.sodium.client.render.chunk.shader.*;
import me.jellysquid.mods.sodium.client.render.chunk.vertex.format.ChunkMeshFormats;
import me.jellysquid.mods.sodium.client.render.chunk.vertex.format.ChunkVertexType;
import net.minecraft.util.Identifier;

import java.util.Map;

public abstract class ShaderChunkRenderer implements ChunkRenderer {
    private final Map<ChunkShaderOptions, GlProgram<ChunkShaderInterface>> programs = new Object2ObjectOpenHashMap<>();

    protected final ChunkVertexType vertexType;
    protected final GlVertexFormat<?> vertexFormat;

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

    private GlProgram.Builder bindAttributesForType(GlProgram.Builder builder) {
        if (this.vertexType == ChunkMeshFormats.COMPACT) {
            return builder.bindAttribute("in_VertexData", ChunkShaderBindingPoints.ATTRIBUTE_PACKED_DATA);
        } else if (this.vertexType == ChunkMeshFormats.VANILLA_LIKE) {
            return builder
                    .bindAttribute("in_Pos", ChunkShaderBindingPoints.ATTRIBUTE_POSITION)
                    .bindAttribute("in_Color", ChunkShaderBindingPoints.ATTRIBUTE_COLOR)
                    .bindAttribute("in_TextureUv", ChunkShaderBindingPoints.ATTRIBUTE_TEXTURE_UV)
                    .bindAttribute("in_DrawParamsLight", ChunkShaderBindingPoints.ATTRIBUTE_DRAW_PARAMS_LIGHT);
        } else
            throw new IllegalArgumentException("Unexpected vertex type");
    }

    private GlProgram<ChunkShaderInterface> createShader(String path, ChunkShaderOptions options) {
        ShaderConstants constants = options.constants();

        GlShader vertShader = ShaderLoader.loadShader(ShaderType.VERTEX,
                new Identifier("sodium", path + ".vsh"), constants);
        
        GlShader fragShader = ShaderLoader.loadShader(ShaderType.FRAGMENT,
                new Identifier("sodium", path + ".fsh"), constants);

        try {
            return bindAttributesForType(GlProgram.builder(new Identifier("sodium", "chunk_shader"))
                    .attachShader(vertShader)
                    .attachShader(fragShader))
                    .bindFragmentData("out_FragColor", ChunkShaderBindingPoints.FRAG_COLOR)
                    .link((shader) -> new ChunkShaderInterface(shader, options));
        } finally {
            vertShader.delete();
            fragShader.delete();
        }
    }

    protected void begin(TerrainRenderPass pass) {
        pass.startDrawing();

        ChunkShaderOptions options = new ChunkShaderOptions(ChunkFogMode.SMOOTH, pass, vertexType);

        this.activeProgram = this.compileProgram(options);
        this.activeProgram.bind();
        this.activeProgram.getInterface()
                .setupState();
    }

    protected void end(TerrainRenderPass pass) {
        this.activeProgram.unbind();
        this.activeProgram = null;

        pass.endDrawing();
    }

    @Override
    public void delete(CommandList commandList) {
        this.programs.values()
                .forEach(GlProgram::delete);
    }

}
