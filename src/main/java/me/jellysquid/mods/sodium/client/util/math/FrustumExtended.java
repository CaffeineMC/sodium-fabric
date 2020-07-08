package me.jellysquid.mods.sodium.client.util.math;

public interface FrustumExtended {
    boolean fastAabbTest(float minX, float minY, float minZ, float maxX, float maxY, float maxZ);
}
