package me.jellysquid.mods.sodium.render.chunk.passes;

import me.jellysquid.mods.thingl.pipeline.RenderPipeline;
import me.jellysquid.mods.thingl.pipeline.options.TranslucencyMode;

public class DefaultBlockRenderPasses {
    public static final BlockRenderPass SOLID = new BlockRenderPass(RenderPipeline.defaults(),
            false, false);
    public static final BlockRenderPass CUTOUT = new BlockRenderPass(RenderPipeline.defaults(),
            false, true);
    public static final BlockRenderPass DETAIL = new BlockRenderPass(RenderPipeline.defaults(),
            false, true);

    public static final BlockRenderPass TRANSLUCENT = new BlockRenderPass(RenderPipeline.builder()
            .setTranslucencyMode(TranslucencyMode.ENABLED)
            .build(), true, false);
}
