package me.jellysquid.mods.sodium.client.model.vertex;

public interface VertexSink {
    void ensureCapacity(int count);

    void flush();
}
