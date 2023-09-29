package me.jellysquid.mods.sodium.client.render.particle;

import me.jellysquid.mods.sodium.client.render.texture.ParticleTextureRegistry;
import net.caffeinemc.mods.sodium.api.buffer.UnmanagedBufferBuilder;
import net.minecraft.client.render.Camera;

public interface BillboardExtended {
    void sodium$buildParticleData(UnmanagedBufferBuilder builder, ParticleTextureRegistry registry, Camera camera, float tickDelta);
}
