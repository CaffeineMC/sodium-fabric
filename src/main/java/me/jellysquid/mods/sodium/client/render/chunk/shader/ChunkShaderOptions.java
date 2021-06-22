package me.jellysquid.mods.sodium.client.render.chunk.shader;

import me.jellysquid.mods.sodium.client.gl.shader.ShaderConstants;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class ChunkShaderOptions {
    public ChunkFogMode fogMode = ChunkFogMode.NONE;

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else if (o == null || getClass() != o.getClass()) {
            return false;
        }

        ChunkShaderOptions that = (ChunkShaderOptions) o;

        return this.fogMode == that.fogMode;
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.fogMode);
    }

    public ShaderConstants constants() {
        List<String> defines = new ArrayList<>();
        defines.addAll(this.fogMode.getDefines());

        return ShaderConstants.fromStringList(defines);
    }
}
