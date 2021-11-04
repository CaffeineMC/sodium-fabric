package me.jellysquid.mods.sodium.client.render.chunk.shader;

import me.jellysquid.mods.sodium.client.gl.shader.ShaderConstants;
import me.jellysquid.mods.sodium.client.render.chunk.passes.BlockRenderPass;

public record ChunkShaderOptions(ChunkFogMode fog, BlockRenderPass pass) {
    public ShaderConstants constants() {
        ShaderConstants.Builder constants = ShaderConstants.builder();
        constants.addAll(this.fog.getDefines());

        if (this.pass.getAlphaCutoff() != 0.0f) {
            constants.add("ALPHA_CUTOFF", String.valueOf(this.pass.getAlphaCutoff()));
        }

        return constants.build();
    }
}
