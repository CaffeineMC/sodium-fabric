package me.jellysquid.mods.sodium.client.render.chunk.terrain;

import net.minecraft.client.render.RenderLayer;

public class TerrainRenderPass {
    @Deprecated(forRemoval = true)
    private final RenderLayer layer;

    private final boolean useReverseOrder;
    private final boolean fragmentDiscard;

    public TerrainRenderPass(RenderLayer layer, boolean useReverseOrder, boolean allowFragmentDiscard) {
        this.layer = layer;

        this.useReverseOrder = useReverseOrder;
        this.fragmentDiscard = allowFragmentDiscard;
    }

    public boolean isReverseOrder() {
        return this.useReverseOrder;
    }

    @Deprecated
    public void startDrawing() {
        this.layer.startDrawing();
    }

    @Deprecated
    public void endDrawing() {
        this.layer.endDrawing();
    }

    public boolean supportsFragmentDiscard() {
        return this.fragmentDiscard;
    }
}
