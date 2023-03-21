package me.jellysquid.mods.sodium.client.util.frustum;

public interface Frustum {
    /**
     * @return The visibility of an axis-aligned box within the frustum
     */
    boolean testBox(double minX, double minY, double minZ, double maxX, double maxY, double maxZ);
}
