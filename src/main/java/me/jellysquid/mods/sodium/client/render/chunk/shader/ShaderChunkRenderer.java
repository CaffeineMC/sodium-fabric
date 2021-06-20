package me.jellysquid.mods.sodium.client.render.chunk.shader;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import me.jellysquid.mods.sodium.client.gl.arena.GlBufferArena;
import me.jellysquid.mods.sodium.client.gl.attribute.GlVertexFormat;
import me.jellysquid.mods.sodium.client.gl.device.CommandList;
import me.jellysquid.mods.sodium.client.gl.shader.*;
import me.jellysquid.mods.sodium.client.gl.device.RenderDevice;
import me.jellysquid.mods.sodium.client.gl.compat.LegacyFogHelper;
import me.jellysquid.mods.sodium.client.gl.tessellation.GlTessellation;
import me.jellysquid.mods.sodium.client.model.vertex.type.ChunkVertexType;
import me.jellysquid.mods.sodium.client.render.chunk.ChunkRenderer;
import me.jellysquid.mods.sodium.client.render.chunk.format.ChunkMeshAttribute;
import me.jellysquid.mods.sodium.client.render.chunk.passes.BlockRenderPass;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

import java.util.Map;

public abstract class ShaderChunkRenderer implements ChunkRenderer {
    private final Map<BlockRenderPass, Map<ChunkShaderOptions, ChunkProgram>> programs = new Object2ObjectOpenHashMap<>();

    protected final ChunkVertexType vertexType;
    protected final GlVertexFormat<ChunkMeshAttribute> vertexFormat;

    protected final RenderDevice device;

    protected ChunkProgram activeProgram;

    public ShaderChunkRenderer(RenderDevice device, ChunkVertexType vertexType) {
        this.device = device;
        this.vertexType = vertexType;
        this.vertexFormat = vertexType.getCustomVertexFormat();
    }

    // TODO: Generalize shader options
    protected ChunkProgram compileProgram(BlockRenderPass pass, ChunkShaderOptions options) {
        Map<ChunkShaderOptions, ChunkProgram> programs = this.programs.get(pass);

        if (programs == null) {
            this.programs.put(pass, programs = new Object2ObjectOpenHashMap<>());
        }

        ChunkProgram program = programs.get(options);

        if (program == null) {
            programs.put(options, program = this.createShader(this.device, getShaderName(pass), options));
        }

        return program;
    }

    // TODO: Define these in the render pass itself
    protected String getShaderName(BlockRenderPass pass) {
        switch (pass) {
            case CUTOUT:
                return "blocks/block_layer_cutout";
            case CUTOUT_MIPPED:
                return "blocks/block_layer_cutout_mipped";
            case TRANSLUCENT:
            case TRIPWIRE:
                return "blocks/block_layer_translucent";
            default:
                return "blocks/block_layer_solid";
        }
    }

    private ChunkProgram createShader(RenderDevice device, String path, ChunkShaderOptions options) {
        ShaderConstants constants = options.constants();

        GlShader vertShader = ShaderLoader.loadShader(device, ShaderType.VERTEX,
                new Identifier("sodium", path + ".vsh"), constants);
        
        GlShader fragShader = ShaderLoader.loadShader(device, ShaderType.FRAGMENT,
                new Identifier("sodium", path + ".fsh"), constants);

        try {
            return GlProgram.builder(new Identifier("sodium", "chunk_shader"))
                    .attachShader(vertShader)
                    .attachShader(fragShader)
                    .bindAttribute("a_Pos", ChunkShaderBindingPoints.POSITION)
                    .bindAttribute("a_Color", ChunkShaderBindingPoints.COLOR)
                    .bindAttribute("a_TexCoord", ChunkShaderBindingPoints.TEX_COORD)
                    .bindAttribute("a_LightCoord", ChunkShaderBindingPoints.LIGHT_COORD)
                    .bindAttribute("d_ModelOffset", ChunkShaderBindingPoints.MODEL_OFFSET)
                    .bindFragmentData("fragColor", ChunkShaderBindingPoints.FRAG_COLOR)
                    .build((name) -> new ChunkProgram(device, name, options));
        } finally {
            vertShader.delete();
            fragShader.delete();
        }
    }

    protected void begin(BlockRenderPass pass, MatrixStack matrixStack) {
        ChunkShaderOptions options = new ChunkShaderOptions();
        options.fogMode = LegacyFogHelper.getFogMode();

        this.activeProgram = this.compileProgram(pass, options);
        this.activeProgram.bind();
        this.activeProgram.setup(matrixStack, this.vertexType.getModelScale(), this.vertexType.getTextureScale());
    }

    protected void end() {
        this.activeProgram.unbind();
        this.activeProgram = null;
    }

    protected abstract GlTessellation createRegionTessellation(CommandList commandList, GlBufferArena vertices, GlBufferArena indices);

    @Override
    public void delete() {
        this.programs.values()
                .stream()
                .flatMap(i -> i.values().stream())
                .forEach(GlProgram::delete);
        this.programs.clear();
    }

    @Override
    public ChunkVertexType getVertexType() {
        return this.vertexType;
    }
}
