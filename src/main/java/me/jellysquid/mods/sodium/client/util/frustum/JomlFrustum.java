package me.jellysquid.mods.sodium.client.util.frustum;

import org.joml.FrustumIntersection;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * Default frustum implementation which extracts planes from a model-view-projection matrix.
 */
public class JomlFrustum implements Frustum {
    private final FrustumIntersection intersection;
    private final Vector3f offset;

    /**
     * @param matrix The model-view-projection matrix of the camera
     * @param offset The position of the frustum in the world
     */
    public JomlFrustum(Matrix4f matrix, Vector3f offset) {
        this.intersection = new FrustumIntersection(matrix, false);
        this.offset = offset;
    }

    @Override
    public Visibility testBox(float minX, float minY, float minZ, float maxX, float maxY, float maxZ) {
        return switch (this.intersection.intersectAab(minX - this.offset.x, minY - this.offset.y, minZ - this.offset.z,
                maxX - this.offset.x, maxY - this.offset.y, maxZ - this.offset.z)) {
            case FrustumIntersection.INTERSECT -> Visibility.INTERSECT;
            case FrustumIntersection.INSIDE -> Visibility.INSIDE;
            default -> Visibility.OUTSIDE;
        };
    }
}
