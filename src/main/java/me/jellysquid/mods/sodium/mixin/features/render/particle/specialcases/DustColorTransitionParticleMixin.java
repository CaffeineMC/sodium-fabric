package me.jellysquid.mods.sodium.mixin.features.render.particle.specialcases;

import me.jellysquid.mods.sodium.client.gl.arena.staging.StagingBufferBuilder;
import me.jellysquid.mods.sodium.client.render.particle.cache.ParticleTextureCache;
import me.jellysquid.mods.sodium.mixin.features.render.particle.BillboardParticleMixin;
import net.caffeinemc.mods.sodium.api.buffer.UnmanagedBufferBuilder;
import net.minecraft.client.particle.DustColorTransitionParticle;
import net.minecraft.client.render.Camera;
import net.minecraft.client.world.ClientWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(DustColorTransitionParticle.class)
public abstract class DustColorTransitionParticleMixin extends BillboardParticleMixin {
    @Shadow
    protected abstract void updateColor(float tickDelta);

    protected DustColorTransitionParticleMixin(ClientWorld world, double x, double y, double z) {
        super(world, x, y, z);
    }

    @Override
    public void sodium$buildParticleData(StagingBufferBuilder builder, ParticleTextureCache registry, Camera camera, float tickDelta) {
        this.updateColor(tickDelta);
        super.sodium$buildParticleData(builder, registry, camera, tickDelta);
    }
}
