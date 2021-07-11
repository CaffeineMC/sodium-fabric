package me.jellysquid.mods.sodium.client.model.vertex.transformers;

import me.jellysquid.mods.sodium.client.model.vertex.formats.ModelQuadVertexSink;

public class DualModelQuadVertexSink implements ModelQuadVertexSink {
    private final ModelQuadVertexSink first;
    private final ModelQuadVertexSink second;

    public DualModelQuadVertexSink(ModelQuadVertexSink first, ModelQuadVertexSink second) {
        this.first = first;
        this.second = second;
    }

    @Override
    public void ensureCapacity(int count) {
        this.first.ensureCapacity(count);
        this.second.ensureCapacity(count);
    }

    @Override
    public void flush() {
        this.first.flush();
        this.second.flush();
    }

    @Override
    public int getVertexCount() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void writeQuad(float x, float y, float z, int color, float u, float v, int light, int overlay, int normal) {
        this.first.writeQuad(x, y, z, color, u, v, light, overlay, normal);
        this.second.writeQuad(x, y, z, color, u, v, light, overlay, normal);
    }
}
