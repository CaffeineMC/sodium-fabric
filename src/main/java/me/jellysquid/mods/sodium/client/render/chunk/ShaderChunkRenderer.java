package me.jellysquid.mods.sodium.client.render.chunk;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import me.jellysquid.mods.sodium.client.gl.device.RenderDevice;
import me.jellysquid.mods.sodium.client.gl.shader.*;
import me.jellysquid.mods.sodium.client.render.chunk.passes.BlockRenderLayer;
import me.jellysquid.mods.sodium.client.render.chunk.shader.*;
import net.minecraft.util.Identifier;

import java.util.Map;

public abstract class ShaderChunkRenderer<T extends ChunkShaderInterface> implements ChunkRenderer {
    private final Map<ChunkShaderOptions, GlProgram<T>> programs = new Object2ObjectOpenHashMap<>();

    protected final RenderDevice device;
    protected final String shaderName;

    protected GlProgram<T> activeProgram;

    public ShaderChunkRenderer(RenderDevice device, String shaderName) {
        this.device = device;
        this.shaderName = shaderName;
    }

    protected GlProgram<T> compileProgram(ChunkShaderOptions options) {
        GlProgram<T> program = this.programs.get(options);

        if (program == null) {
            this.programs.put(options, program = this.createShader(this.shaderName, options));
        }

        return program;
    }

    private GlProgram<T> createShader(String path, ChunkShaderOptions options) {
        ShaderConstants constants = options.constants();

        GlShader vertShader = ShaderLoader.loadShader(ShaderType.VERTEX,
                new Identifier("sodium", path + ".vsh"), constants);

        GlShader fragShader = ShaderLoader.loadShader(ShaderType.FRAGMENT,
                new Identifier("sodium", path + ".fsh"), constants);

        try {
            return GlProgram.builder(new Identifier("sodium", "chunk_shader"))
                    .attachShader(vertShader)
                    .attachShader(fragShader)
                    .bindFragmentData("fragColor", ChunkShaderBindingPoints.FRAG_COLOR)
                    .link((shader) -> this.createShaderInterface(shader, options));
        } finally {
            vertShader.delete();
            fragShader.delete();
        }
    }

    protected abstract T createShaderInterface(ShaderBindingContext context, ChunkShaderOptions options);

    protected void begin(BlockRenderLayer pass) {
        ChunkShaderOptions options = new ChunkShaderOptions(ChunkFogMode.SMOOTH, pass);

        this.activeProgram = this.compileProgram(options);
        this.activeProgram.bind();
        this.activeProgram.getInterface()
                .setup();
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
}
