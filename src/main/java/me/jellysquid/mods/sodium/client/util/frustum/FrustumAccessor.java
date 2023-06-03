package me.jellysquid.mods.sodium.client.util.frustum;

import net.minecraft.client.render.Frustum;
import org.joml.FrustumIntersection;
import org.joml.Vector3f;

public interface FrustumAccessor {
    static FrustumAccessor of(Frustum frustum) {
        return (FrustumAccessor) frustum;
    }

    Vector3f getTranslation();
    FrustumIntersection getFrustumIntersection();
}
