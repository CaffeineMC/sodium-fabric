package me.jellysquid.mods.sodium.client.render;

import me.jellysquid.mods.sodium.client.model.vertex.formats.generic.PositionTextureSink;
import me.jellysquid.mods.sodium.client.model.vertex.formats.ModelQuadVertexSink;

public class ModelQuadOverlayAdapter implements ModelQuadVertexSink {
    private final PositionTextureSink sink;

    public ModelQuadOverlayAdapter(PositionTextureSink sink) {
        this.sink = sink;
    }

    @Override
    public void ensureCapacity(int count) {
        this.sink.ensureCapacity(count);
    }

    @Override
    public void flush() {
        this.sink.flush();
    }

    @Override
    public int getVertexCount() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void writeQuad(float x, float y, float z, int color, float u, float v, int light, int overlay, int normal) {
        this.sink.writeVertex(x, y, z, u, v);
    }
}
