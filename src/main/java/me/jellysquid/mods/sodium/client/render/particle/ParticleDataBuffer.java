package me.jellysquid.mods.sodium.client.render.particle;

import me.jellysquid.mods.sodium.client.gl.buffer.GlBufferTexture;
import net.caffeinemc.mods.sodium.api.buffer.UnmanagedBufferBuilder;

public class ParticleDataBuffer {
    private final GlBufferTexture bufferTexture;

    public ParticleDataBuffer() {
        this.bufferTexture = new GlBufferTexture();
    }

    public void uploadParticleData(UnmanagedBufferBuilder.Built data) {
        this.bufferTexture.putData(data.buffer, data.size);
    }

    public void bind() {
        this.bufferTexture.bind();
    }
}
