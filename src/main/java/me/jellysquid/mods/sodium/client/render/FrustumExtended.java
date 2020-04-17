package me.jellysquid.mods.sodium.client.render;

public interface FrustumExtended {
    boolean fastAabbTest(float minX, float minY, float minZ, float maxX, float maxY, float maxZ);
}
