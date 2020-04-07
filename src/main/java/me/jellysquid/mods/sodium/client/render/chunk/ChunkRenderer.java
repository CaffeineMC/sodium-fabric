package me.jellysquid.mods.sodium.client.render.chunk;

import me.jellysquid.mods.sodium.client.render.chunk.compile.ChunkRender;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.util.math.MatrixStack;

public interface ChunkRenderer<T extends ChunkRenderData> {
    T createRenderData();

    void begin(MatrixStack matrixStack);

    void render(ChunkRender<T> chunk, RenderLayer layer, MatrixStack matrixStack, double x, double y, double z);

    void end(MatrixStack matrixStack);
}
