package me.jellysquid.mods.sodium.mixin.features.render.particle.specialcases;

import me.jellysquid.mods.sodium.client.render.particle.cache.ParticleTextureCache;
import me.jellysquid.mods.sodium.mixin.features.render.particle.BillboardParticleMixin;
import net.caffeinemc.mods.sodium.api.buffer.UnmanagedBufferBuilder;
import net.minecraft.client.particle.FireworksSparkParticle;
import net.minecraft.client.render.Camera;
import net.minecraft.client.world.ClientWorld;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(FireworksSparkParticle.Flash.class)
public abstract class FlashParticleMixin extends BillboardParticleMixin {
    protected FlashParticleMixin(ClientWorld world, double x, double y, double z) {
        super(world, x, y, z);
    }

    @Override
    public void sodium$buildParticleData(UnmanagedBufferBuilder builder, ParticleTextureCache registry, Camera camera, float tickDelta) {
        this.setAlpha(0.6F - ((float)this.age + tickDelta - 1.0F) * 0.25F * 0.5F);
        super.sodium$buildParticleData(builder, registry, camera, tickDelta);
    }
}
