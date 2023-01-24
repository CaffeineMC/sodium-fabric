package me.jellysquid.mods.sodium.client.render.vertex.serializers;

public interface VertexSerializer {
    void serialize(long srcBuffer, int srcStride, long dstBuffer, int dstStride, int count);
}
