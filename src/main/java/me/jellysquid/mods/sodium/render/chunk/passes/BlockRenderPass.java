package me.jellysquid.mods.sodium.render.chunk.passes;

import me.jellysquid.mods.thingl.pipeline.RenderPipeline;

public class BlockRenderPass {
    private final RenderPipeline pipeline;
    private final boolean translucent;

    BlockRenderPass(RenderPipeline pipeline, boolean translucent) {
        this.pipeline = pipeline;
        this.translucent = translucent;
    }

    public boolean isTranslucent() {
        return this.translucent;
    }

    public RenderPipeline pipeline() {
        return this.pipeline;
    }

    @Deprecated
    public boolean isDetail() {
        return this == DefaultBlockRenderPasses.DETAIL;
    }
}
