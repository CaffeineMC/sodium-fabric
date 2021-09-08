package me.jellysquid.mods.sodium.render.chunk.passes;

import me.jellysquid.mods.thingl.pipeline.RenderPipeline;
import me.jellysquid.mods.thingl.pipeline.options.TranslucencyMode;

public class DefaultBlockRenderPasses {
    public static final BlockRenderPass OPAQUE = new BlockRenderPass(RenderPipeline.defaults(), false);
    public static final BlockRenderPass DETAIL = new BlockRenderPass(RenderPipeline.defaults(), false);

    public static final BlockRenderPass TRANSLUCENT = new BlockRenderPass(RenderPipeline.builder()
            .setTranslucencyMode(TranslucencyMode.ENABLED)
            .build(), true);
}
