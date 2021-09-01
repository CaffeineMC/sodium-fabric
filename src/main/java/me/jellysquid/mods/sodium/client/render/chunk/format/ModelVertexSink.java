package me.jellysquid.mods.sodium.client.render.chunk.format;

import me.jellysquid.mods.sodium.client.model.vertex.VertexSink;
import net.minecraft.util.math.Vec3i;

public interface ModelVertexSink extends VertexSink {
    /**
     * Writes a quad vertex to this sink.
     * @param posX The x-position of the vertex
     * @param posY The y-position of the vertex
     * @param posZ The z-position of the vertex
     * @param color The ABGR-packed color of the vertex
     * @param u The u-texture of the vertex
     * @param v The y-texture of the vertex
     * @param light The packed light-map coordinates of the vertex
     * @param chunkId
     * @param bits
     */
    void writeVertex(float posX, float posY, float posZ, int color, float u, float v, int light, int chunkId, int bits);

    default void writeVertex(Vec3i offset, float posX, float posY, float posZ, int color, float u, float v, int light, int chunkId, int bits) {
        this.writeVertex(offset.getX() + posX, offset.getY() + posY, offset.getZ() + posZ, color, u, v, light, chunkId, bits);
    }

}
