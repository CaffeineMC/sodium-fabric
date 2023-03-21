package me.jellysquid.mods.sodium.mixin.core;

import me.jellysquid.mods.sodium.client.util.frustum.FrustumAdapter;
import net.minecraft.client.render.Frustum;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(Frustum.class)
public abstract class MixinFrustum implements FrustumAdapter, me.jellysquid.mods.sodium.client.util.frustum.Frustum {
    @Shadow
    public abstract boolean isVisible(double minX, double minY, double minZ, double maxX, double maxY, double maxZ);

    @Override
    public me.jellysquid.mods.sodium.client.util.frustum.Frustum sodium$createFrustum() {
        return this;
    }

    @Override
    public boolean testBox(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        return this.isVisible(minX, minY, minZ, maxX, maxY, maxZ);
    }
}
