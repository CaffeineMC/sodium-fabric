package me.jellysquid.mods.sodium.client.render.chunk.passes;

import net.minecraft.client.render.RenderLayer;

public record BlockRenderLayer(RenderLayer renderLayer, boolean translucent, float alphaCutoff) {
    public static final BlockRenderLayer SOLID = new BlockRenderLayer(RenderLayer.getSolid(), false, 0.0f);
    public static final BlockRenderLayer CUTOUT = new BlockRenderLayer(RenderLayer.getCutout(), false, 0.1f);
    public static final BlockRenderLayer CUTOUT_MIPPED = new BlockRenderLayer(RenderLayer.getCutoutMipped(), false, 0.5f);
    public static final BlockRenderLayer TRANSLUCENT = new BlockRenderLayer(RenderLayer.getTranslucent(), true, 0.0f);
    public static final BlockRenderLayer TRIPWIRE = new BlockRenderLayer(RenderLayer.getTripwire(), true, 0.1f);
}