package me.jellysquid.mods.sodium.client.render.chunk.shader;

import me.jellysquid.mods.sodium.client.gl.shader.ShaderConstants;
import me.jellysquid.mods.sodium.client.render.chunk.passes.BlockRenderLayer;

public record ChunkShaderOptions(ChunkFogMode fog, BlockRenderLayer pass) {
    public ShaderConstants constants() {
        ShaderConstants.Builder constants = ShaderConstants.builder();
        constants.addAll(this.fog.getDefines());

        if (this.pass.alphaCutoff() != 0.0f) {
            constants.add("ALPHA_CUTOFF", String.valueOf(this.pass.alphaCutoff()));
        }

        return constants.build();
    }
}
