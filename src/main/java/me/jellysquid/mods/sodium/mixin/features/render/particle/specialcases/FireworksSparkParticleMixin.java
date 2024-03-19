package me.jellysquid.mods.sodium.mixin.features.render.particle.specialcases;

import me.jellysquid.mods.sodium.client.gl.arena.staging.StagingBufferBuilder;
import me.jellysquid.mods.sodium.client.render.particle.cache.ParticleTextureCache;
import me.jellysquid.mods.sodium.mixin.features.render.particle.BillboardParticleMixin;
import net.caffeinemc.mods.sodium.api.buffer.UnmanagedBufferBuilder;
import net.minecraft.client.particle.FireworksSparkParticle;
import net.minecraft.client.render.Camera;
import net.minecraft.client.world.ClientWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(FireworksSparkParticle.Explosion.class)
public abstract class FireworksSparkParticleMixin extends BillboardParticleMixin {
    @Shadow
    private boolean flicker;

    protected FireworksSparkParticleMixin(ClientWorld world, double x, double y, double z) {
        super(world, x, y, z);
    }

    @Override
    public void sodium$buildParticleData(StagingBufferBuilder builder, ParticleTextureCache registry, Camera camera, float tickDelta) {
        if (!this.flicker || this.age < this.maxAge / 3 || (this.age + this.maxAge) / 3 % 2 == 0) {
            super.sodium$buildParticleData(builder, registry, camera, tickDelta);
        }
    }
}
