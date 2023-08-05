package me.jellysquid.mods.sodium.client.render.viewport;

import org.joml.FrustumIntersection;

public final class Viewport {
    private final FrustumIntersection[] frustums;
    private final double x, y, z;

    public Viewport(FrustumIntersection[] frustums, double x, double y, double z) {
        this.frustums = frustums;

        this.x = x;
        this.y = y;
        this.z = z;
    }

    public boolean isBoxVisible(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        for (var frustum : this.frustums) {
            var result = frustum.testAab((float) (minX - this.x), (float) (minY - this.y), (float) (minZ - this.z),
                    (float) (maxX - this.x), (float) (maxY - this.y), (float) (maxZ - this.z));

            // early-exit if not inside one of the frustums
            if (!result) {
                return false;
            }
        }

        // passed all frustum checks
        return true;
    }
}
