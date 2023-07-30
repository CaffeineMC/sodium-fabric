package me.jellysquid.mods.sodium.mixin.core;

import me.jellysquid.mods.sodium.client.util.frustum.FrustumAccessor;
import net.minecraft.client.render.Frustum;
import org.joml.FrustumIntersection;
import org.joml.Vector3d;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(Frustum.class)
public abstract class MixinFrustum implements FrustumAccessor {
    @Shadow
    @Final
    private FrustumIntersection frustumIntersection;

    @Shadow
    private double x;

    @Shadow
    private double y;

    @Shadow
    private double z;

    @Override
    public Vector3d getTranslation() {
        return new Vector3d(this.x, this.y, this.z);
    }

    @Override
    public FrustumIntersection getFrustumIntersection() {
        return this.frustumIntersection;
    }
}
