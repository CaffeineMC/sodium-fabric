package me.jellysquid.mods.sodium.client.render.layer;

import net.minecraft.client.render.RenderLayer;

// TODO: Move away from using an enum, make this extensible
public enum BlockRenderPass {
    SOLID(RenderLayer.getSolid(), false),
    CUTOUT(RenderLayer.getCutout(), false),
    CUTOUT_MIPPED(RenderLayer.getCutoutMipped(), false),
    TRANSLUCENT(RenderLayer.getTranslucent(), true);

    public static final BlockRenderPass[] VALUES = BlockRenderPass.values();

    private final RenderLayer layer;
    private final boolean translucent;

    BlockRenderPass(RenderLayer layer, boolean translucent) {
        this.layer = layer;
        this.translucent = translucent;
    }

    public static int count() {
        return VALUES.length;
    }

    public boolean isTranslucent() {
        return this.translucent;
    }

    public void endDrawing() {
        this.layer.endDrawing();
    }

    public void startDrawing() {
        this.layer.startDrawing();
    }
}
