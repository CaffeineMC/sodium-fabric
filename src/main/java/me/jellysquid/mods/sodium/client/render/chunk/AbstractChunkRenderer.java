package me.jellysquid.mods.sodium.client.render.chunk;

import me.jellysquid.mods.sodium.client.render.chunk.compile.ChunkRender;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;

public abstract class AbstractChunkRenderer<T extends ChunkRenderData> implements ChunkRenderer<T> {
    protected void beginChunkRender(MatrixStack matrixStack, ChunkRender<T> chunk, double x, double y, double z) {
        BlockPos origin = chunk.getOrigin();

        matrixStack.push();
        matrixStack.translate((double) origin.getX() - x, (double) origin.getY() - y, (double) origin.getZ() - z);
    }

    protected void endChunkRender(MatrixStack matrixStack, ChunkRender<T> chunk, double x, double y, double z) {
        matrixStack.pop();
    }
}
