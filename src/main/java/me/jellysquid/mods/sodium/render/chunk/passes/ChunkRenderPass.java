package me.jellysquid.mods.sodium.render.chunk.passes;

import me.jellysquid.mods.sodium.opengl.types.RenderState;

public record ChunkRenderPass(RenderState renderState, boolean mipped, float alphaCutoff) {
    public boolean usesReverseOrder() {
        return this.renderState.blendFunction != null;
    }

    public boolean isCutout() {
        return this.alphaCutoff != 0.0f;
    }
}
