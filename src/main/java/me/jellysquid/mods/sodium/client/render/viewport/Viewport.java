package me.jellysquid.mods.sodium.client.render.viewport;

import org.joml.FrustumIntersection;

public class Viewport {
    private final FrustumIntersection[] frustums;
    private final float x, y, z;

    public Viewport(FrustumIntersection[] frustums, float x, float y, float z) {
        this.frustums = frustums;

        this.x = x;
        this.y = y;
        this.z = z;
    }

    public boolean isBoxVisible(float minX, float minY, float minZ, float maxX, float maxY, float maxZ) {
        for (var frustum : this.frustums) {
            var result = frustum.testAab(minX - this.x, minY - this.y, minZ - this.z,
                    maxX - this.x, maxY - this.y, maxZ - this.z);

            // early-exit if not inside one of the frustums
            if (!result) {
                return false;
            }
        }

        // passed all frustum checks
        return true;
    }
}
