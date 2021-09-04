package me.jellysquid.mods.sodium.render.chunk.passes;

import net.minecraft.client.render.RenderLayer;

public class DefaultBlockRenderPasses {
    public static final BlockRenderPass OPAQUE = new BlockRenderPass(RenderLayer.getSolid(), false);
    public static final BlockRenderPass DETAIL = new BlockRenderPass(RenderLayer.getCutout(), false);
    public static final BlockRenderPass TRANSLUCENT = new BlockRenderPass(RenderLayer.getTranslucent(), true);
}
