package me.jellysquid.mods.sodium.client.render.backends;

import me.jellysquid.mods.sodium.client.render.chunk.ChunkRender;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.util.math.MatrixStack;

public interface ChunkRenderBackend<T extends ChunkRenderState> {
    T createRenderState();

    void begin(MatrixStack matrixStack);

    void render(ChunkRender<T> chunk, RenderLayer layer, MatrixStack matrixStack, double x, double y, double z);

    void end(MatrixStack matrixStack);
}
