package net.caffeinemc.sodium.interop.vanilla.math.frustum;

public interface Frustum {
    /**
     * @return The visibility of an axis-aligned box within the frustum
     */
    int intersectBox(float minX, float minY, float minZ, float maxX, float maxY, float maxZ, int skipMask);
    
    /**
     * @return The visibility of an axis-aligned box within the frustum
     */
    default int intersectBox(float minX, float minY, float minZ, float maxX, float maxY, float maxZ) {
        return this.intersectBox(minX, minY, minZ, maxX, maxY, maxZ, BLANK_RESULT);
    }

    /**
     * @return true if the axis-aligned box is visible within the frustum, otherwise false
     */
    default boolean containsBox(float minX, float minY, float minZ, float maxX, float maxY, float maxZ, int skipMask) {
        return this.intersectBox(minX, minY, minZ, maxX, maxY, maxZ, skipMask) != Frustum.OUTSIDE;
    }
    
    /**
     * @return true if the axis-aligned box is visible within the frustum, otherwise false
     */
    default boolean containsBox(float minX, float minY, float minZ, float maxX, float maxY, float maxZ) {
        return this.containsBox(minX, minY, minZ, maxX, maxY, maxZ, BLANK_RESULT);
    }
    
    /**
     * Return value indicating that the axis-aligned box is fully inside the frustum.
     */
    int INSIDE = 0b111111;
    
    /**
     * Return value indicating that the axis-aligned box is completely outside the frustum.
     */
    int OUTSIDE = -1;
    
    /**
     * Supplied for the skipMask when executing a box test without any prior data.
     */
    int BLANK_RESULT = 0;
}
