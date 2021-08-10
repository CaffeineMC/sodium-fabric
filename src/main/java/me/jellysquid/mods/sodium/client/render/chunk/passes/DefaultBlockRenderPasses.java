package me.jellysquid.mods.sodium.client.render.chunk.passes;

import net.minecraft.client.render.RenderLayer;

public class DefaultBlockRenderPasses {
    public static final BlockRenderPass SOLID = new BlockRenderPass(RenderLayer.getSolid(), false, 0.0f);
    public static final BlockRenderPass CUTOUT = new BlockRenderPass(RenderLayer.getCutout(), false, 0.1f);
    public static final BlockRenderPass CUTOUT_MIPPED = new BlockRenderPass(RenderLayer.getCutoutMipped(), false, 0.5f);
    public static final BlockRenderPass TRANSLUCENT = new BlockRenderPass(RenderLayer.getTranslucent(), true, 0.0f);
    public static final BlockRenderPass TRIPWIRE = new BlockRenderPass(RenderLayer.getTripwire(), true, 0.1f);
}
