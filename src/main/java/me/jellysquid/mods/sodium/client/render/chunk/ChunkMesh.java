package me.jellysquid.mods.sodium.client.render.chunk;

import me.jellysquid.mods.sodium.client.gl.buffer.VertexData;
import me.jellysquid.mods.sodium.client.render.layer.BlockRenderPass;
import org.apache.commons.lang3.Validate;

public class ChunkMesh {
    /**
     * The render pass this chunk mesh is rendered on.
     */
    private final BlockRenderPass renderPass;

    /**
     * The pending data to be uploaded to graphics memory. If this is null, then the mesh is considered up-to-date.
     */
    private VertexData pendingUpload;

    public ChunkMesh(BlockRenderPass renderPass, VertexData pendingUpload) {
        Validate.notNull(pendingUpload);

        this.pendingUpload = pendingUpload;
        this.renderPass = renderPass;
    }

    /**
     * Takes the pending upload for this chunk mesh, marking it as as up-to-date. If no data is pending upload, this
     * method throws an exception.
     *
     * @return The data which is pending upload to graphics memory
     */
    public VertexData takePendingUpload() {
        VertexData data = this.pendingUpload;

        if (data == null) {
            throw new NullPointerException("No pending data to upload");
        }

        this.pendingUpload = null;

        return data;
    }

    public BlockRenderPass getRenderPass() {
        return this.renderPass;
    }
}
