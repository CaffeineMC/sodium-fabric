package net.caffeinemc.sodium.render.chunk.passes;

import net.caffeinemc.gfx.api.pipeline.PipelineDescription;
import net.minecraft.util.Identifier;

public record ChunkRenderPass(PipelineDescription pipelineDescription, boolean mipped, float alphaCutoff, Identifier id) {
    public boolean usesReverseOrder() {
        return this.pipelineDescription.blendFunc != null;
    }

    public boolean isCutout() {
        return this.alphaCutoff != 0.0f;
    }

    @Override
    public String toString() {
        return this.id.toString();
    }
}
