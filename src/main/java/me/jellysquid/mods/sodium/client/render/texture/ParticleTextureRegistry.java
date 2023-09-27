package me.jellysquid.mods.sodium.client.render.texture;

import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;

public class ParticleTextureRegistry {
    private final Long2IntOpenHashMap uvToIndex = new Long2IntOpenHashMap();

    public int get(float u, float v) {
        long key = ((long) Float.floatToRawIntBits(u)) << 32 | ((long) Float.floatToRawIntBits(v));
        return uvToIndex.get(key);
    }
}
