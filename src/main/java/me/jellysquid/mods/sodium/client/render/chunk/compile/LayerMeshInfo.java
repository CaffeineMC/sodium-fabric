package me.jellysquid.mods.sodium.client.render.chunk.compile;

import me.jellysquid.mods.sodium.client.render.vertex.BufferUploadData;
import net.minecraft.client.render.RenderLayer;
import org.apache.commons.lang3.Validate;

public class LayerMeshInfo {
    private final RenderLayer renderLayer;
    private BufferUploadData pendingUpload;

    public LayerMeshInfo(RenderLayer renderLayer, BufferUploadData pendingUpload) {
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
