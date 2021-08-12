package me.jellysquid.mods.sodium.client.render.chunk.passes;

import net.minecraft.client.render.RenderLayer;

// TODO: Move away from using an enum, make this extensible
public enum BlockRenderPass {
    SOLID(RenderLayer.getSolid(), false, 0.0f),
    CUTOUT(RenderLayer.getCutout(), false, 0.1f),
    CUTOUT_MIPPED(RenderLayer.getCutoutMipped(), false, 0.5f),
    TRANSLUCENT(RenderLayer.getTranslucent(), true, 0.0f),
    TRIPWIRE(RenderLayer.getTripwire(), true, 0.1f);

    public static final BlockRenderPass[] VALUES = BlockRenderPass.values();
    public static final int COUNT = VALUES.length;

    private final RenderLayer layer;
    private final boolean translucent;
    private final float alphaCutoff;

    BlockRenderPass(RenderLayer layer, boolean translucent, float alphaCutoff) {
        this.layer = layer;
        this.translucent = translucent;
        this.alphaCutoff = alphaCutoff;
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

    public float getAlphaCutoff() {
        return this.alphaCutoff;
    }
}
