package me.jellysquid.mods.sodium.client.render.chunk.passes;

import net.minecraft.client.render.RenderLayer;

// TODO: Move away from using an enum, make this extensible
public enum BlockRenderPass {
    SOLID(RenderLayer.getSolid(), false),
    CUTOUT(RenderLayer.getCutout(), false),
    CUTOUT_MIPPED(RenderLayer.getCutoutMipped(), false),
    TRANSLUCENT(RenderLayer.getTranslucent(), true),
    TRIPWIRE(RenderLayer.getTripwire(), true);

    public static final BlockRenderPass[] VALUES = BlockRenderPass.values();
    public static final int COUNT = VALUES.length;

    private final RenderLayer layer;
    private final boolean translucent;

    BlockRenderPass(RenderLayer layer, boolean translucent) {
        this.layer = layer;
        this.translucent = translucent;
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
