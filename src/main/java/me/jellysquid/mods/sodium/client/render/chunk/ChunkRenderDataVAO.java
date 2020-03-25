package me.jellysquid.mods.sodium.client.render.chunk;

import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import me.jellysquid.mods.sodium.client.render.gl.GlVertexArray;
import me.jellysquid.mods.sodium.client.render.gl.GlVertexBuffer;
import me.jellysquid.mods.sodium.client.render.vertex.BufferUploadData;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import org.lwjgl.opengl.GL15;

import java.util.HashMap;
import java.util.Map;

public class ChunkRenderDataVAO implements ChunkRenderData {
    private static final VertexFormat VERTEX_FORMAT = VertexFormats.POSITION_COLOR_TEXTURE_LIGHT_NORMAL;

    private final HashMap<RenderLayer, VertexBufferWithArray> vaos = new HashMap<>();

    public ChunkRenderDataVAO() {
        for (RenderLayer layer : RenderLayer.getBlockLayers()) {
            this.vaos.put(layer, new VertexBufferWithArray(VERTEX_FORMAT, new GlVertexBuffer(GL15.GL_ARRAY_BUFFER), new GlVertexArray()));
        }
    }

    public VertexBufferWithArray getVertexArrayForLayer(RenderLayer layer) {
        return this.vaos.get(layer);
    }

    @Override
    public void destroy() {
        for (VertexBufferWithArray buffer : this.vaos.values()) {
            buffer.delete();
        }
    }

    @Override
    public void uploadMeshes(Object2ObjectMap<RenderLayer, BufferUploadData> layers) {
        for (Map.Entry<RenderLayer, BufferUploadData> entry : layers.entrySet()) {
            VertexBufferWithArray array = this.vaos.get(entry.getKey());

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
