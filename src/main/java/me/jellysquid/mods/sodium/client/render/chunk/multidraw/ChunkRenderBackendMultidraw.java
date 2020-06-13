package me.jellysquid.mods.sodium.client.render.chunk.multidraw;

import me.jellysquid.mods.sodium.client.gl.SodiumVertexFormats;
import me.jellysquid.mods.sodium.client.gl.attribute.GlVertexFormat;
import me.jellysquid.mods.sodium.client.gl.shader.GlShader;
import me.jellysquid.mods.sodium.client.gl.shader.ShaderConstants;
import me.jellysquid.mods.sodium.client.gl.shader.ShaderLoader;
import me.jellysquid.mods.sodium.client.gl.shader.ShaderType;
import me.jellysquid.mods.sodium.client.render.chunk.ChunkGraphicsState;
import me.jellysquid.mods.sodium.client.render.chunk.shader.ChunkFogMode;
import me.jellysquid.mods.sodium.client.render.chunk.shader.ChunkRenderShaderBackend;
import net.minecraft.util.Identifier;

public abstract class ChunkRenderBackendMultidraw<T extends ChunkGraphicsState> extends ChunkRenderShaderBackend<T, ChunkProgramMultidraw> {
    private final int maxBatchSize;

    public ChunkRenderBackendMultidraw(GlVertexFormat<SodiumVertexFormats.ChunkMeshAttribute> format, int maxBatchSize) {
        super(format);

        this.maxBatchSize = maxBatchSize;
    }

    @Override
    protected ChunkProgramMultidraw createShaderProgram(Identifier name, int handle, ChunkFogMode fogMode) {
        return new ChunkProgramMultidraw(name, handle, fogMode.getFactory());
    }

    @Override
    protected GlShader createVertexShader(ChunkFogMode fogMode) {
        return ShaderLoader.loadShader(ShaderType.VERTEX, new Identifier("sodium", "chunk_gl20.v.glsl"),
                this.createShaderConstants(fogMode));
    }

    @Override
    protected GlShader createFragmentShader(ChunkFogMode fogMode) {
        return ShaderLoader.loadShader(ShaderType.FRAGMENT, new Identifier("sodium", "chunk_gl20.f.glsl"),
                this.createShaderConstants(fogMode));
    }

    private ShaderConstants createShaderConstants(ChunkFogMode fogMode) {
        ShaderConstants.Builder constants = ShaderConstants.builder();
        constants.define("USE_MULTIDRAW");
        constants.define("MAX_BATCH_SIZE", String.valueOf(this.maxBatchSize));

        fogMode.addConstants(constants);

        return constants.build();
    }
}
