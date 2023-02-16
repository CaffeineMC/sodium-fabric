package me.jellysquid.mods.sodium.client.util.frustum;

import static org.joml.FrustumIntersection.OUTSIDE;

public interface Frustum {
    /**
     * @return The visibility of an axis-aligned box within the frustum
     */
    int testBox(float minX, float minY, float minZ, float maxX, float maxY, float maxZ);

    /**
     * @return true if the axis-aligned box is visible within the frustum, otherwise false
     */
    default boolean isBoxVisible(float minX, float minY, float minZ, float maxX, float maxY, float maxZ) {
        return this.testBox(minX, minY, minZ, maxX, maxY, maxZ) != OUTSIDE;
    }
}
