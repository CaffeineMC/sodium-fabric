package me.jellysquid.mods.sodium.client.render.vertex.serializers;

public interface VertexSerializer {
    void serialize(long src, long dst, int count);
}
