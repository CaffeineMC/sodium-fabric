package me.jellysquid.mods.sodium.client.render.layer;

import net.minecraft.client.render.RenderLayer;

public class BlockRenderPass {
    public static final BlockRenderPass FALLBACK = new BlockRenderPass(RenderLayer.getCutoutMipped(), false);

    private final RenderLayer layer;
    private final boolean translucent;

    public BlockRenderPass(RenderLayer layer, boolean translucent) {
        this.layer = layer;
        this.translucent = translucent;
    }

    public RenderLayer getRenderLayer() {
        return this.layer;
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
