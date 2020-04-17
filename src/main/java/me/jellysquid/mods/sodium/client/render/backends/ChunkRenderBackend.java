package me.jellysquid.mods.sodium.client.render.backends;

import me.jellysquid.mods.sodium.client.render.chunk.ChunkBuildResult;
import me.jellysquid.mods.sodium.client.render.layer.BlockRenderPass;
import net.minecraft.client.util.math.MatrixStack;

import java.util.Iterator;

public interface ChunkRenderBackend<T extends ChunkRenderState> {
    void upload(Iterator<ChunkBuildResult<T>> queue);

    void render(Iterator<T> renders, BlockRenderPass layer, MatrixStack matrixStack, double x, double y, double z);

    void delete();

    Class<T> getRenderStateType();
}
