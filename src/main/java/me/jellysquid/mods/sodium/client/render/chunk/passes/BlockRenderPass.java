package me.jellysquid.mods.sodium.client.render.chunk.passes;

import net.minecraft.client.render.RenderLayer;

public class BlockRenderPass {
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
