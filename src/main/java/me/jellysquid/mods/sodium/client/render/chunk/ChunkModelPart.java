package me.jellysquid.mods.sodium.client.render.chunk;

import me.jellysquid.mods.sodium.client.model.quad.ModelQuadFacing;
import me.jellysquid.mods.sodium.client.render.chunk.passes.BlockRenderPass;

public class ChunkModelPart {
    public final int start, count;

    public ChunkModelPart(int start, int count) {
        this.start = start;
        this.count = count;
    }

    public static byte encodeKey(BlockRenderPass pass, ModelQuadFacing facing) {
        return (byte) ((pass.ordinal() * ModelQuadFacing.COUNT) + facing.ordinal());
    }

    public static int count() {
        return ModelQuadFacing.COUNT * BlockRenderPass.COUNT;
    }
}
