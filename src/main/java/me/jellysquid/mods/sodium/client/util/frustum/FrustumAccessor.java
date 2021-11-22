package me.jellysquid.mods.sodium.client.util.frustum;

import net.minecraft.util.math.Matrix4f;
import net.minecraft.util.math.Vec3d;

public interface FrustumAccessor {
    Matrix4f getProjectionMatrix();

    Matrix4f getModelViewMatrix();

    Vec3d getPosition();
}
