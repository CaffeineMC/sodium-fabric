package net.caffeinemc.sodium.interop.vanilla.math.frustum;

import org.joml.FrustumIntersection;

public interface Frustum {
    /**
     * @return The visibility of an axis-aligned box within the frustum
     */
    int testBox(float minX, float minY, float minZ, float maxX, float maxY, float maxZ, int skipMask);

    /**
     * @return true if the axis-aligned box is visible within the frustum, otherwise false
     */
    default boolean isBoxVisible(float minX, float minY, float minZ, float maxX, float maxY, float maxZ) {
        return this.testBox(minX, minY, minZ, maxX, maxY, maxZ, 0) != Frustum.OUTSIDE;
    }
    
    /**
     * Return value indicating that the axis-aligned box is fully inside the frustum.
     */
    int INSIDE = 0b11_1111;
    
    /**
     * Return value indicating that the axis-aligned box is completely outside the frustum.
     */
    int OUTSIDE = -1;
    
    /**
     * Supplied for the skipMask when executing a box test without any prior data.
     */
    int BLANK_RESULT = 0;
}
