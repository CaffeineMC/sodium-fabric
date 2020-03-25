package me.jellysquid.mods.sodium.client.render.chunk;

import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import me.jellysquid.mods.sodium.client.render.gl.GlVertexBuffer;
import me.jellysquid.mods.sodium.client.render.vertex.BufferUploadData;
import net.minecraft.client.render.RenderLayer;

import java.util.HashMap;
import java.util.Map;

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
    public void uploadMeshes(Object2ObjectMap<RenderLayer, BufferUploadData> layers) {
        for (Map.Entry<RenderLayer, BufferUploadData> entry : layers.entrySet()) {
            GlVertexBuffer array = this.vbos.get(entry.getKey());

            if (array == null) {
                throw new NullPointerException("No graphics state container for layer " + entry.getKey());
            }

            array.upload(entry.getValue());
        }
    }

    @Override
    public void deleteMeshes() {

    }
}
