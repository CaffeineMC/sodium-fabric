package me.jellysquid.mods.sodium.render.chunk.passes;

import net.minecraft.client.render.RenderLayer;

public class BlockRenderPass {
    private final RenderLayer layer;
    private final boolean translucent;

    BlockRenderPass(RenderLayer layer, boolean translucent) {
        this.layer = layer;
        this.translucent = translucent;
    }

    public boolean isTranslucent() {
        return this.translucent;
    }

    @Deprecated(forRemoval = true)
    public void endDrawing() {
        this.layer.endDrawing();
    }

    @Deprecated(forRemoval = true)
    public void startDrawing() {
        this.layer.startDrawing();
    }

    @Deprecated
    public boolean isDetail() {
        return this == DefaultBlockRenderPasses.DETAIL;
    }
}
