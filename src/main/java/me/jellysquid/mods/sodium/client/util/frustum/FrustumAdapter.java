package me.jellysquid.mods.sodium.client.util.frustum;

import me.jellysquid.mods.sodium.client.util.math.JomlHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.FrustumIntersection;
import org.joml.Matrix4f;

/**
 * Default frustum implementation which extracts the matrices from the Minecraft frustum class.
 */
public class FrustumAdapter implements Frustum {
    private final FrustumIntersection intersection;

    public FrustumAdapter(FrustumAccessor accessor) {
        Matrix4f matrix = new Matrix4f();
        matrix.set(JomlHelper.copy(accessor.getProjectionMatrix()));
        matrix.mul(JomlHelper.copy(accessor.getModelViewMatrix()));

        Vec3d pos = accessor.getPosition();
        matrix.translate((float) -pos.x, (float) -pos.y, (float) -pos.z);

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
