package me.jellysquid.mods.sodium.client.render.particle;

import net.caffeinemc.mods.sodium.api.buffer.UnmanagedBufferBuilder;
import net.minecraft.client.render.Camera;

public interface BillboardExtended {
    void sodium$buildParticleData(UnmanagedBufferBuilder builder, Camera camera, float tickDelta);
}
