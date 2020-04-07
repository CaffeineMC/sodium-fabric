package me.jellysquid.mods.sodium.client.render.chunk;

import me.jellysquid.mods.sodium.client.gl.BufferUploadData;
import net.minecraft.client.render.RenderLayer;
import org.apache.commons.lang3.Validate;

public class ChunkLayerInfo {
    private final RenderLayer renderLayer;
    private BufferUploadData pendingUpload;

    public ChunkLayerInfo(RenderLayer renderLayer, BufferUploadData pendingUpload) {
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

    public RenderLayer getLayer() {
        return this.renderLayer;
    }
}
