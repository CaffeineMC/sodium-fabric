package me.jellysquid.mods.sodium.client.render.chunk;

import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.util.math.MatrixStack;

public interface ChunkRenderer<T extends ChunkRenderData> {
    T createRenderData();

    void begin();

    void render(MatrixStack matrixStack, RenderLayer layer, T chunk);

    void end();
}
