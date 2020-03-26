package me.jellysquid.mods.sodium.client.render.chunk;

import me.jellysquid.mods.sodium.client.render.chunk.compile.ChunkMeshInfo;
import me.jellysquid.mods.sodium.client.render.gl.GlVertexBuffer;
import net.minecraft.client.render.RenderLayer;

import java.util.HashMap;

public class ChunkRenderDataVBO implements ChunkRenderData {
    private final HashMap<RenderLayer, GlVertexBuffer> vbos = new HashMap<>();

    public GlVertexBuffer getVertexBufferForLayer(RenderLayer layer) {
        return this.vbos.get(layer);
    }

    @Override
    public void destroy() {
        for (GlVertexBuffer buffer : this.vbos.values()) {
            buffer.delete();
        }
    }

    @Override
    public void uploadChunk(ChunkMeshInfo mesh) {
        for (ChunkMeshInfo.MeshUpload entry : mesh.getUploads()) {
            GlVertexBuffer array = this.vbos.get(entry.layer);

            if (array == null) {
                throw new NullPointerException("No graphics state container for layer " + entry.layer);
            }

            array.upload(entry.data);
        }
    }

    @Override
    public void deleteMeshes() {

    }
}
