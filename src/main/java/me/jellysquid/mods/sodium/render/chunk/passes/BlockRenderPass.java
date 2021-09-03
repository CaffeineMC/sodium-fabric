package me.jellysquid.mods.sodium.render.chunk.passes;

import net.minecraft.client.render.RenderLayer;

// TODO: Move away from using an enum, make this extensible
public enum BlockRenderPass {
    OPAQUE(RenderLayer.getCutout(), false),
    OPAQUE_DETAIL(RenderLayer.getCutout(), false),
    TRANSLUCENT(RenderLayer.getTranslucent(), true);

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

    public RenderLayer getLayer() {
        return this.layer;
    }

    @Deprecated
    public void endDrawing() {
        this.layer.endDrawing();
    }

    @Deprecated
    public void startDrawing() {
        this.layer.startDrawing();
    }

    public boolean isDetail() {
        return this == OPAQUE_DETAIL;
    }
}
