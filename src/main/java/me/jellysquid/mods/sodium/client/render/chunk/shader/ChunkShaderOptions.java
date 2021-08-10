package me.jellysquid.mods.sodium.client.render.chunk.shader;

import me.jellysquid.mods.sodium.client.gl.shader.ShaderConstants;
import me.jellysquid.mods.sodium.client.render.chunk.passes.BlockRenderPass;

import java.util.ArrayList;
import java.util.List;

public record ChunkShaderOptions(ChunkFogMode fog, BlockRenderPass pass) {
    public ShaderConstants constants() {
        List<String> defines = new ArrayList<>();
        defines.addAll(this.fog.getDefines());

        if (this.pass.getAlphaCutoff() != 0.0f) {
            defines.add("ALPHA_CUTOFF " + this.pass.getAlphaCutoff());
        }

        return ShaderConstants.fromStringList(defines);
    }
}
