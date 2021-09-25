package me.jellysquid.mods.sodium.client.render.chunk.passes;

import net.minecraft.client.renderer.RenderType;

// TODO: Move away from using an enum, make this extensible
public enum BlockRenderPass {
    SOLID(RenderType.solid(), false, 0.0f),
    CUTOUT(RenderType.cutout(), false, 0.1f),
    CUTOUT_MIPPED(RenderType.cutoutMipped(), false, 0.5f),
    TRANSLUCENT(RenderType.translucent(), true, 0.0f),
    TRIPWIRE(RenderType.tripwire(), true, 0.1f);

    public static final BlockRenderPass[] VALUES = BlockRenderPass.values();
    public static final int COUNT = VALUES.length;

    private final RenderType type;
    private final boolean translucent;
    private final float alphaCutoff;

    BlockRenderPass(RenderType type, boolean translucent, float alphaCutoff) {
        this.type = type;
        this.translucent = translucent;
        this.alphaCutoff = alphaCutoff;
    }

    public boolean isTranslucent() {
        return this.translucent;
    }

    public RenderType getType() {
        return this.type;
    }

    @Deprecated
    public void endDrawing() {
        this.type.clearRenderState();
    }

    @Deprecated
    public void startDrawing() {
        this.type.setupRenderState();
    }

    public float getAlphaCutoff() {
        return this.alphaCutoff;
    }
}
