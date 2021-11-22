package me.jellysquid.mods.sodium.client.util.frustum;

import org.joml.FrustumIntersection;
import org.joml.Matrix4f;

/**
 * Default frustum implementation which extracts planes from a model-view-projection matrix.
 */
public class CameraFrustum implements Frustum {
    private final FrustumIntersection intersection;

    /**
     * @param matrix The model-view-projection matrix of the camera
     */
    public CameraFrustum(Matrix4f matrix) {
        this.intersection = new FrustumIntersection(matrix, false);
    }

    @Override
    public Visibility testBox(float minX, float minY, float minZ, float maxX, float maxY, float maxZ) {
        return switch (this.intersection.intersectAab(minX, minY, minZ, maxX, maxY, maxZ)) {
            case FrustumIntersection.INTERSECT -> Visibility.INTERSECT;
            case FrustumIntersection.INSIDE -> Visibility.INSIDE;
            default -> Visibility.OUTSIDE;
        };
    }
}
