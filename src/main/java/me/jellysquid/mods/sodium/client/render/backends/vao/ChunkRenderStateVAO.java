package me.jellysquid.mods.sodium.client.render.backends.vao;

import it.unimi.dsi.fastutil.objects.Reference2ReferenceArrayMap;
import me.jellysquid.mods.sodium.client.gl.GlVertexArray;
import me.jellysquid.mods.sodium.client.gl.GlVertexArrayBuffer;
import me.jellysquid.mods.sodium.client.gl.GlVertexBuffer;
import me.jellysquid.mods.sodium.client.render.backends.ChunkRenderState;
import me.jellysquid.mods.sodium.client.render.chunk.ChunkLayerInfo;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import org.lwjgl.opengl.GL15;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class ChunkRenderStateVAO implements ChunkRenderState {
    private static final VertexFormat VERTEX_FORMAT = VertexFormats.POSITION_COLOR_TEXTURE_LIGHT_NORMAL;

    private final Reference2ReferenceArrayMap<RenderLayer, GlVertexArrayBuffer> vaos = new Reference2ReferenceArrayMap<>();

    public GlVertexArrayBuffer getVertexArrayForLayer(RenderLayer layer) {
        return this.vaos.get(layer);
    }

    @Override
    public void clearData() {
        for (GlVertexArrayBuffer buffer : this.vaos.values()) {
            buffer.delete();
        }

        this.vaos.clear();
    }

    @Override
    public void uploadData(Collection<ChunkLayerInfo> layers) {
        List<RenderLayer> removed = new ArrayList<>(this.vaos.keySet());

        for (ChunkLayerInfo entry : layers) {
            GlVertexArrayBuffer buffer = this.vaos.computeIfAbsent(entry.getLayer(), this::createData);
            buffer.upload(entry.takePendingUpload());

            removed.remove(entry.getLayer());
        }

        for (RenderLayer layer : removed) {
            GlVertexArrayBuffer buffer = this.vaos.remove(layer);
            buffer.delete();
        }
    }

    private GlVertexArrayBuffer createData(RenderLayer layer) {
        return new GlVertexArrayBuffer(VERTEX_FORMAT, new GlVertexBuffer(GL15.GL_ARRAY_BUFFER), new GlVertexArray());
    }
}
