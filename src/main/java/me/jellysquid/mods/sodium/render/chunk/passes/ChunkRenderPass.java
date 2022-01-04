package me.jellysquid.mods.sodium.render.chunk.passes;

import me.jellysquid.mods.sodium.opengl.types.RenderPipeline;
import me.jellysquid.mods.sodium.opengl.types.TranslucencyMode;

public record ChunkRenderPass(RenderPipeline pipeline, boolean mipped, float alphaCutoff) {
    public boolean isTranslucent() {
        return this.pipeline.translucencyMode == TranslucencyMode.ENABLED;
    }
}
