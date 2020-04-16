package me.jellysquid.mods.sodium.client.render.chunk;

import me.jellysquid.mods.sodium.client.gl.buffer.BufferUploadData;
import me.jellysquid.mods.sodium.client.render.layer.BlockRenderPass;
import org.apache.commons.lang3.Validate;

public class ChunkMesh {
    private final BlockRenderPass renderLayer;
    private BufferUploadData pendingUpload;

    public ChunkMesh(BlockRenderPass renderLayer, BufferUploadData pendingUpload) {
        Validate.notNull(pendingUpload);

        this.pendingUpload = pendingUpload;
        this.renderLayer = renderLayer;
    }

    public BufferUploadData takePendingUpload() {
        BufferUploadData data = this.pendingUpload;

        if (data == null) {
            throw new NullPointerException("No pending data to upload");
        }

        this.pendingUpload = null;

        return data;
    }

    public BlockRenderPass getLayer() {
        return this.renderLayer;
    }
}
