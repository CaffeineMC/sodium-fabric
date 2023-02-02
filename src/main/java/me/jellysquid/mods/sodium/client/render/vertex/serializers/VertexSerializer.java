package me.jellysquid.mods.sodium.client.render.vertex.serializers;

public interface VertexSerializer {
    void serialize(long srcBuffer, long dstBuffer, int count);
}
