package me.jellysquid.mods.sodium.client.render.chunk.multidraw;

import me.jellysquid.mods.sodium.client.gl.SodiumVertexFormats;
import me.jellysquid.mods.sodium.client.gl.attribute.GlVertexFormat;
import me.jellysquid.mods.sodium.client.gl.shader.ShaderConstants;
import me.jellysquid.mods.sodium.client.render.chunk.ChunkGraphicsState;
import me.jellysquid.mods.sodium.client.render.chunk.shader.ChunkProgramComponentBuilder;
import me.jellysquid.mods.sodium.client.render.chunk.shader.ChunkRenderShaderBackend;
import net.minecraft.util.Identifier;

public abstract class ChunkRenderBackendMultiDraw<T extends ChunkGraphicsState> extends ChunkRenderShaderBackend<T, ChunkProgramMultiDraw> {
    public ChunkRenderBackendMultiDraw(GlVertexFormat<SodiumVertexFormats.ChunkMeshAttribute> format) {
        super(format);
    }

    @Override
    protected ChunkProgramMultiDraw createShaderProgram(Identifier name, int handle, ChunkProgramComponentBuilder components) {
        return new ChunkProgramMultiDraw(name, handle, components);
    }


    @Override
    protected void addShaderConstants(ShaderConstants.Builder builder) {
        builder.define("USE_MULTIDRAW");
    }
}
