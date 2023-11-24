package me.jellysquid.mods.sodium.client.render.particle;

import me.jellysquid.mods.sodium.client.gl.buffer.GlBufferTexture;
import me.jellysquid.mods.sodium.client.gl.device.CommandList;
import net.caffeinemc.mods.sodium.api.buffer.UnmanagedBufferBuilder;

public class ParticleBuffers {
    private final GlBufferTexture particleData;
    private final GlBufferTexture textureCache;

    public ParticleBuffers(CommandList commandList) {
        this.particleData = new GlBufferTexture(commandList, 3);
        this.textureCache = new GlBufferTexture(commandList, 4);
    }

    public void uploadParticleData(CommandList commandList, UnmanagedBufferBuilder.Built data, UnmanagedBufferBuilder.Built cache) {
        this.particleData.putData(commandList, data.buffer, data.size);
        this.textureCache.putData(commandList, cache.buffer, cache.size);
    }

    public void bind() {
        this.particleData.bind();
        this.textureCache.bind();
    }
}
