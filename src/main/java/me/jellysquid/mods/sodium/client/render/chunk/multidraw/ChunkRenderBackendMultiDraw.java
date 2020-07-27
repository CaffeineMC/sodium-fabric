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

public abstract class ChunkRenderBackendMultiDraw<T extends ChunkGraphicsState> extends ChunkRenderShaderBackend<T, ChunkProgramMultiDraw> {
    public ChunkRenderBackendMultiDraw(GlVertexFormat<SodiumVertexFormats.ChunkMeshAttribute> format) {
        super(format);
    }

    @Override
    protected ChunkProgramMultiDraw createShaderProgram(Identifier name, int handle, ChunkFogMode fogMode, boolean useCulling) {
        return new ChunkProgramMultiDraw(name, handle, fogMode.getFactory(), useCulling);
    }

    @Override
    protected GlShader createVertexShader(ChunkFogMode fogMode, boolean useCulling) {
        return ShaderLoader.loadShader(ShaderType.VERTEX, new Identifier("sodium", "chunk_gl20.v.glsl"),
                this.createShaderConstants(fogMode, useCulling));
    }

    @Override
    protected GlShader createFragmentShader(ChunkFogMode fogMode, boolean useCulling) {
        return ShaderLoader.loadShader(ShaderType.FRAGMENT, new Identifier("sodium", "chunk_gl20.f.glsl"),
                this.createShaderConstants(fogMode, useCulling));
    }

    private ShaderConstants createShaderConstants(ChunkFogMode fogMode, boolean useCulling) {
        ShaderConstants.Builder constants = ShaderConstants.builder();
        constants.define("USE_MULTIDRAW");

        fogMode.addConstants(constants);

        if (useCulling) {
            constants.define("USE_CULLING");
        }

        return constants.build();
    }
}
