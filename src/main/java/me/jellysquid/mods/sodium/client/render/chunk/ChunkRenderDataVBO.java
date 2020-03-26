package me.jellysquid.mods.sodium.client.render.chunk;

import it.unimi.dsi.fastutil.objects.Reference2ReferenceArrayMap;
import me.jellysquid.mods.sodium.client.render.chunk.compile.ChunkMeshInfo;
import me.jellysquid.mods.sodium.client.render.gl.GlVertexBuffer;
import net.minecraft.client.render.RenderLayer;
import org.lwjgl.opengl.GL15;

public class ChunkRenderDataVBO implements ChunkRenderData {
    private final Reference2ReferenceArrayMap<RenderLayer, GlVertexBuffer> vbos = new Reference2ReferenceArrayMap<>();

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
            GlVertexBuffer array = this.vbos.computeIfAbsent(entry.layer, this::createData);
            array.upload(entry.data);
        }
    }

    private GlVertexBuffer createData(RenderLayer layer) {
        return new GlVertexBuffer(GL15.GL_ARRAY_BUFFER);
    }
}
