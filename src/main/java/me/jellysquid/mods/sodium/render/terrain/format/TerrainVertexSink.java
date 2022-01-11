package me.jellysquid.mods.sodium.render.terrain.format;

import me.jellysquid.mods.sodium.render.vertex.VertexSink;
import net.minecraft.util.math.Vec3i;

public interface TerrainVertexSink extends VertexSink {
    /**
     * Writes a quad vertex to this sink.
     * @param posX The x-position of the vertex
     * @param posY The y-position of the vertex
     * @param posZ The z-position of the vertex
     * @param color The ABGR-packed color of the vertex
     * @param u The u-texture of the vertex
     * @param v The y-texture of the vertex
     * @param light The packed light-map coordinates of the vertex
     */
    void writeVertex(float posX, float posY, float posZ, int color, float u, float v, int light);

    default void writeVertex(Vec3i offset, float posX, float posY, float posZ, int color, float u, float v, int light) {
        this.writeVertex(offset.getX() + posX, offset.getY() + posY, offset.getZ() + posZ, color, u, v, light);
    }

}
