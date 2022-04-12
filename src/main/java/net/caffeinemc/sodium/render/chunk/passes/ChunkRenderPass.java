package net.caffeinemc.sodium.render.chunk.passes;

import net.caffeinemc.gfx.api.pipeline.PipelineDescription;

public record ChunkRenderPass(PipelineDescription pipelineDescription, boolean mipped, float alphaCutoff, String shaderName) {
    public boolean usesReverseOrder() {
        return this.pipelineDescription.blendFunc != null;
    }

    public boolean isCutout() {
        return this.alphaCutoff != 0.0f;
    }
}
