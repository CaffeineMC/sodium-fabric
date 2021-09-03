package me.jellysquid.mods.sodium.render.chunk.shader;

public enum ChunkShaderTextureUnit {
    BLOCK_TEXTURE(0),
    LIGHT_TEXTURE(2),
    STIPPLE_TEXTURE(9);

    private final int id;

    ChunkShaderTextureUnit(int id) {
        this.id = id;
    }

    public int id() {
        return this.id;
    }
}
