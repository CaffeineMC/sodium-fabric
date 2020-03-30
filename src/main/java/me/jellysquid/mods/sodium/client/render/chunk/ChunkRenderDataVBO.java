package me.jellysquid.mods.sodium.client.render.chunk;

import it.unimi.dsi.fastutil.objects.Reference2ReferenceArrayMap;
import me.jellysquid.mods.sodium.client.render.chunk.compile.ChunkMeshInfo;
import me.jellysquid.mods.sodium.client.render.chunk.compile.LayerMeshInfo;
import me.jellysquid.mods.sodium.client.render.gl.GlVertexBuffer;
import net.minecraft.client.render.RenderLayer;
import org.lwjgl.opengl.GL15;

import java.util.ArrayList;
import java.util.List;

public class ChunkRenderDataVBO implements ChunkRenderData {
    private final Reference2ReferenceArrayMap<RenderLayer, GlVertexBuffer> vbos = new Reference2ReferenceArrayMap<>();

    public GlVertexBuffer getVertexBufferForLayer(RenderLayer layer) {
        return this.vbos.get(layer);
    }

    @Override
    public void clearData() {
        for (GlVertexBuffer buffer : this.vbos.values()) {
            buffer.delete();
        }

        this.vbos.clear();
    }

    @Override
    public void uploadData(ChunkMeshInfo mesh) {
        List<RenderLayer> removed = new ArrayList<>(this.vbos.keySet());

        for (LayerMeshInfo entry : mesh.getLayers()) {
            GlVertexBuffer buffer = this.vbos.computeIfAbsent(entry.getLayer(), this::createData);
            buffer.upload(entry.takePendingUpload());

            removed.remove(entry.getLayer());
        }

        for (RenderLayer layer : removed) {
            GlVertexBuffer buffer = this.vbos.remove(layer);
            buffer.delete();
        }
    }

    private GlVertexBuffer createData(RenderLayer layer) {
        return new GlVertexBuffer(GL15.GL_ARRAY_BUFFER);
    }
}
