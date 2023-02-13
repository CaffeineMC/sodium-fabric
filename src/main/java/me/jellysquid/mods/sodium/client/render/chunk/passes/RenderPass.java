package me.jellysquid.mods.sodium.client.render.chunk.passes;

import net.minecraft.client.render.RenderLayer;

public class RenderPass {
    @Deprecated(forRemoval = true)
    private final RenderLayer layer;

    private final boolean useReverseOrder;
    private final boolean fragmentDiscard;

    public RenderPass(RenderLayer layer, boolean useReverseOrder, boolean allowFragmentDiscard) {
        this.layer = layer;

        this.useReverseOrder = useReverseOrder;
        this.fragmentDiscard = allowFragmentDiscard;
    }

    public boolean isReverseOrder() {
        return this.useReverseOrder;
    }

    @Deprecated
    public void begin() {
        this.layer.startDrawing();
    }

    @Deprecated
    public void end() {
        this.layer.endDrawing();
    }

    public boolean supportsFragmentDiscard() {
        return this.fragmentDiscard;
    }
}
