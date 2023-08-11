package me.jellysquid.mods.sodium.client.render.viewport;

import org.joml.FrustumIntersection;

public final class Viewport {
    private final FrustumIntersection frustum;
    private final CameraTransform transform;

    public Viewport(FrustumIntersection frustum, double x, double y, double z) {
        this.frustum = frustum;
        this.transform = new CameraTransform(x, y, z);
    }

    public boolean isBoxVisible(int intX, int intY, int intZ, float radius) {
        float floatX = (intX - this.transform.intX) - this.transform.fracX;
        float floatY = (intY - this.transform.intY) - this.transform.fracY;
        float floatZ = (intZ - this.transform.intZ) - this.transform.fracZ;

        return this.frustum.testAab(
                floatX - radius,
                floatY - radius,
                floatZ - radius,

                floatX + radius,
                floatY + radius,
                floatZ + radius
        );
    }
}
