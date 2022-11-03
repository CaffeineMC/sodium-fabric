package me.jellysquid.mods.sodium.mixin.core;

import me.jellysquid.mods.sodium.client.util.frustum.FrustumAdapter;
import net.minecraft.client.render.Frustum;
import org.joml.FrustumIntersection;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(Frustum.class)
public class MixinFrustum implements FrustumAdapter, me.jellysquid.mods.sodium.client.util.frustum.Frustum {
    @Shadow
    private double x;

    @Shadow
    private double y;

    @Shadow
    private double z;

    @Shadow
    @Final
    private FrustumIntersection frustumIntersection;

    @Override
    public me.jellysquid.mods.sodium.client.util.frustum.Frustum sodium$createFrustum() {
        return this;
    }

    @Override
    public Visibility testBox(float minX, float minY, float minZ, float maxX, float maxY, float maxZ) {
        return switch (this.frustumIntersection.intersectAab(minX - (float) this.x, minY - (float) this.y, minZ - (float) this.z,
                maxX - (float) this.x, maxY - (float) this.y, maxZ - (float) this.z)) {
            case FrustumIntersection.INTERSECT -> Visibility.INTERSECT;
            case FrustumIntersection.INSIDE -> Visibility.INSIDE;
            default -> Visibility.OUTSIDE;
        };
    }
}
