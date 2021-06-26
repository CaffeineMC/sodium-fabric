package me.jellysquid.mods.sodium.client.render.chunk.format;

import me.jellysquid.mods.sodium.client.model.vertex.VertexSink;
import net.minecraft.util.math.Vec3i;

public interface ModelVertexSink extends VertexSink {
    /**
     * Writes a quad vertex to this sink.
     * @param offsetX The x-position of the vertex's integer offset
     * @param offsetY The y-position of the vertex's integer offset
     * @param offsetZ The z-position of the vertex's integer offset
     * @param posX The x-position of the vertex
     * @param posY The y-position of the vertex
     * @param posZ The z-position of the vertex
     * @param color The ABGR-packed color of the vertex
     * @param u The u-texture of the vertex
     * @param v The y-texture of the vertex
     * @param light The packed light-map coordinates of the vertex
     */
    void writeVertex(int offsetX, int offsetY, int offsetZ, float posX, float posY, float posZ, int color, float u, float v, int light);

    default void writeVertex(Vec3i offset, float posX, float posY, float posZ, int color, float u, float v, int light) {
        this.writeVertex(offset.getX(), offset.getY(), offset.getZ(), posX, posY, posZ, color, u, v, light);
    }

}
