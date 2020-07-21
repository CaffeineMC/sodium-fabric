package me.jellysquid.mods.sodium.client.render.chunk.shader;

import me.jellysquid.mods.sodium.client.render.chunk.shader.texture.ChunkProgramTextureComponent;

public class ChunkProgramComponentBuilder {
    public ShaderComponent.Factory<ChunkProgramTextureComponent, ChunkProgram> texture;
    public ShaderComponent.Factory<ChunkShaderFogComponent, ChunkProgram> fog;
}
