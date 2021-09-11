package me.jellysquid.mods.sodium.render.chunk.passes;

import me.jellysquid.mods.thingl.pipeline.RenderPipeline;

public class BlockRenderPass {
    private final RenderPipeline pipeline;
    private final boolean translucent;
    private final boolean discard;

    BlockRenderPass(RenderPipeline pipeline, boolean translucent, boolean discard) {
        this.pipeline = pipeline;
        this.translucent = translucent;
        this.discard = discard;
    }

    public boolean isTranslucent() {
        return this.translucent;
    }

    public boolean useDiscard() {
        return this.discard;
    }

    public RenderPipeline pipeline() {
        return this.pipeline;
    }

    @Deprecated
    public boolean isDetail() {
        return this == DefaultBlockRenderPasses.DETAIL;
    }
}
