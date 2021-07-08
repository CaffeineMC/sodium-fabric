package me.jellysquid.mods.sodium.client.render.chunk.shader;

import me.jellysquid.mods.sodium.client.gl.shader.ShaderConstants;

import java.util.ArrayList;
import java.util.List;

public record ChunkShaderOptions(ChunkFogMode fog) {
    public ShaderConstants constants() {
        List<String> defines = new ArrayList<>();
        defines.addAll(this.fog.getDefines());

        return ShaderConstants.fromStringList(defines);
    }
}
