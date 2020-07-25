package me.jellysquid.mods.sodium.client.render.chunk.passes.impl;

import me.jellysquid.mods.sodium.client.render.chunk.passes.BlockLayer;
import me.jellysquid.mods.sodium.client.render.chunk.passes.BlockRenderPass;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.util.Identifier;

public class TranslucentRenderPass extends BlockRenderPass {
    public TranslucentRenderPass(int ordinal, Identifier id, BlockLayer... layers) {
        super(ordinal, id, false, layers);
    }

    @Override
    public void beginRender() {
        RenderLayer.getTranslucent().startDrawing();
    }

    @Override
    public void endRender() {
        RenderLayer.getTranslucent().endDrawing();
    }
}
