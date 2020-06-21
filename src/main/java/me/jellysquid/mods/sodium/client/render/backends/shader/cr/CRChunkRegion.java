package me.jellysquid.mods.sodium.client.render.backends.shader.cr;

import me.jellysquid.mods.sodium.client.gl.array.GlVertexArray;
import me.jellysquid.mods.sodium.client.gl.attribute.GlVertexAttributeBinding;
import me.jellysquid.mods.sodium.client.gl.memory.BufferBlock;
import me.jellysquid.mods.sodium.client.gl.util.GlQueryObject;
import me.jellysquid.mods.sodium.client.gl.util.MultiDrawBatch;
import me.jellysquid.mods.sodium.client.render.backends.shader.lcb.AbstractChunkRegion;
import me.jellysquid.mods.sodium.client.render.backends.shader.lcb.ChunkRegionManager;
import me.jellysquid.mods.sodium.client.render.layer.BlockRenderPass;
import net.minecraft.util.math.ChunkSectionPos;

import java.util.function.Consumer;

public class CRChunkRegion extends AbstractChunkRegion {
    private final ChunkSectionPos origin;
    private final MultiDrawBatch[] batchesForPasses;
    private final BufferBlock buffer;
    private final GlQueryObject queryObject;

    public int discriminator = -1;

    public CRChunkRegion(ChunkSectionPos origin) {
        this.origin = origin;
        this.buffer = new BufferBlock();
        this.batchesForPasses = new MultiDrawBatch[BlockRenderPass.count()];
        this.queryObject = new GlQueryObject();
    }

    public ChunkSectionPos getOrigin() {
        return this.origin;
    }

    public BufferBlock getBuffer() {
        return this.buffer;
    }

    public GlQueryObject getQueryObject() {
        return queryObject;
    }

    @Override
    public boolean isEmpty() {
        return this.buffer.isEmpty();
    }

    @Override
    public void delete() {
        this.buffer.delete();
        this.queryObject.delete();
    }

    public MultiDrawBatch acquireBatchForLayer(BlockRenderPass pass) {
        int ordinal = pass.ordinal();
        if (batchesForPasses[ordinal] == null) {
            batchesForPasses[ordinal] = new MultiDrawBatch(ChunkRegionManager.BUFFER_SIZE);
        }
        return batchesForPasses[ordinal];
    }

    // @Nullable
    public MultiDrawBatch getBatchForLayer(BlockRenderPass pass) {
        int ordinal = pass.ordinal();
        return batchesForPasses[ordinal];
    }

    public void addToBatch(BlockRenderPass pass, int startIndex, int length) {
        this.acquireBatchForLayer(pass).add(startIndex, length);
    }

    public void drawBatch(BlockRenderPass pass, GlVertexAttributeBinding[] attributes) {
        MultiDrawBatch batch = getBatchForLayer(pass);

        if (batch != null) {
            GlVertexArray array = this.buffer.bind(attributes);
            batch.draw();
        }
    }

    public void drawBatch(
            BlockRenderPass pass,
            GlVertexAttributeBinding[] attributes,
            Consumer<Runnable> wrapper
    ) {
        MultiDrawBatch batch = getBatchForLayer(pass);

        if (batch != null) {
            wrapper.accept(() -> {
                GlVertexArray array = this.buffer.bind(attributes);
                batch.draw();
            });
        }
    }

    public boolean isBatchEmpty() {
        for (MultiDrawBatch batch : batchesForPasses) {
            if (batch != null) {
                if (!batch.isEmpty()) {
                    return false;
                }
            }
        }
        return true;
    }

    public void resetBatches() {
        for (MultiDrawBatch batch : batchesForPasses) {
            if (batch != null) {
                batch.reset();
            }
        }
    }


}
