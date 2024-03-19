package me.jellysquid.mods.sodium.client.render.particle;

import me.jellysquid.mods.sodium.client.gl.arena.staging.StagingBufferBuilder;
import me.jellysquid.mods.sodium.client.render.particle.cache.ParticleTextureCache;
import net.caffeinemc.mods.sodium.api.buffer.UnmanagedBufferBuilder;
import net.minecraft.client.render.Camera;

public interface BillboardExtended {
    void sodium$buildParticleData(StagingBufferBuilder builder, ParticleTextureCache registry, Camera camera, float tickDelta);
}
