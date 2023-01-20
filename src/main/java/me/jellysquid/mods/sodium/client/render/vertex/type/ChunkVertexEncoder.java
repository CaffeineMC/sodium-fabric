package me.jellysquid.mods.sodium.client.render.vertex.type;

import net.minecraft.util.math.Vec3i;

public interface ChunkVertexEncoder {
    default long write(long ptr,
                       Vec3i offset, float posX, float posY, float posZ, int color, float u, float v, int light, int chunkId) {
        return this.write(ptr, offset.getX() + posX, offset.getY() + posY, offset.getZ() + posZ, color, u, v, light, chunkId);
    }
    long write(long ptr,
               float posX, float posY, float posZ, int color, float u, float v, int light, int chunkId);
}
