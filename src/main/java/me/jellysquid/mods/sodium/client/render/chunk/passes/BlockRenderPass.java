package me.jellysquid.mods.sodium.client.render.chunk.passes;

import net.minecraft.util.Identifier;

public abstract class BlockRenderPass {
    private final Identifier id;
    private final BlockLayer[] layers;
    private final boolean forward;
    private final int ordinal;

    public BlockRenderPass(int ordinal, Identifier id, boolean forward, BlockLayer... layers) {
        this.ordinal = ordinal;
        this.id = id;
        this.layers = layers;
        this.forward = forward;
    }

    public abstract void beginRender();

    public abstract void endRender();

    public final boolean isForwardRendering() {
        return this.forward;
    }

    public final int ordinal() {
        return this.ordinal;
    }

    public final BlockLayer[] getLayers() {
        return this.layers;
    }

    public final Identifier getId() {
        return this.id;
    }
}
