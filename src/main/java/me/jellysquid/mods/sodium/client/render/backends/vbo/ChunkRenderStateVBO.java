package me.jellysquid.mods.sodium.client.render.backends.vbo;

import it.unimi.dsi.fastutil.objects.Reference2ReferenceArrayMap;
import me.jellysquid.mods.sodium.client.gl.GlVertexBuffer;
import me.jellysquid.mods.sodium.client.render.backends.ChunkRenderState;
import me.jellysquid.mods.sodium.client.render.chunk.ChunkLayerInfo;
import net.minecraft.client.render.RenderLayer;
import org.lwjgl.opengl.GL15;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class ChunkRenderStateVBO implements ChunkRenderState {
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
    public void uploadData(Collection<ChunkLayerInfo> layers) {
        List<RenderLayer> removed = new ArrayList<>(this.vbos.keySet());

        for (ChunkLayerInfo entry : layers) {
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
