package me.jellysquid.mods.sodium.client.util.frustum;

public interface Frustum {
    /**
     * @return The visibility of an axis-aligned box within the frustum
     */
    Visibility testBox(float minX, float minY, float minZ, float maxX, float maxY, float maxZ);

    /**
     * @return true if the axis-aligned box is visible within the frustum, otherwise false
     */
    default boolean isBoxVisible(float minX, float minY, float minZ, float maxX, float maxY, float maxZ) {
        return this.testBox(minX, minY, minZ, maxX, maxY, maxZ) != Visibility.OUTSIDE;
    }

    enum Visibility {
        /**
         * The object is fully outside the frustum and is not visible.
         */
        OUTSIDE,

        /**
         * The object intersects with a plane of the frustum and is visible.
         */
        INTERSECT,

        /**
         * The object is fully contained within the frustum and is visible.
         */
        INSIDE
    }
}
