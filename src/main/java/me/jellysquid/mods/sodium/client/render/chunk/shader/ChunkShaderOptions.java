package me.jellysquid.mods.sodium.client.render.chunk.shader;

import me.jellysquid.mods.thingl.shader.ShaderConstants;
import me.jellysquid.mods.sodium.client.render.chunk.passes.BlockRenderPass;

public record ChunkShaderOptions(ChunkFogMode fog, BlockRenderPass pass) {
    public ShaderConstants constants() {
        ShaderConstants.Builder constants = ShaderConstants.builder();
        constants.addAll(this.fog.getDefines());

        if (this.pass.isDetail()) {
            constants.add("DETAIL");

            if (this.pass == BlockRenderPass.OPAQUE_DETAIL)  {
                constants.add("DETAIL_FADE_IN");
            } else {
                constants.add("DETAIL_FADE_OUT");
            }
        }

        return constants.build();
    }
}
