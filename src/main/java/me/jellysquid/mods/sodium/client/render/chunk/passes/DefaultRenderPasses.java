package me.jellysquid.mods.sodium.client.render.chunk.passes;

import net.minecraft.client.render.RenderLayer;

public class DefaultRenderPasses {
    public static final RenderPass SOLID = new RenderPass(RenderLayer.getSolid(), false, false);
    public static final RenderPass CUTOUT = new RenderPass(RenderLayer.getCutoutMipped(), false, true);
    public static final RenderPass TRANSLUCENT = new RenderPass(RenderLayer.getTranslucent(), true, false);


    public static final RenderPass[] ALL = new RenderPass[] { SOLID, CUTOUT, TRANSLUCENT };
}
