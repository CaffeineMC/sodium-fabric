package net.caffeinemc.mods.sodium.client.render.chunk.terrain;

import net.minecraft.client.renderer.RenderType;

public class TerrainRenderPass {
    @Deprecated(forRemoval = true)
    private final RenderType renderType;

    private final boolean isTranslucent;
    private final boolean fragmentDiscard;

    public TerrainRenderPass(RenderType renderType, boolean isTranslucent, boolean allowFragmentDiscard) {
        this.renderType = renderType;

        this.isTranslucent = isTranslucent;
        this.fragmentDiscard = allowFragmentDiscard;
    }

    public boolean isTranslucent() {
        return this.isTranslucent;
    }

    @Deprecated
    public void startDrawing() {
        this.renderType.setupRenderState();
    }

    @Deprecated
    public void endDrawing() {
        this.renderType.clearRenderState();
    }

    public boolean supportsFragmentDiscard() {
        return this.fragmentDiscard;
    }
}
