package me.jellysquid.mods.sodium.client.render.particle;

import me.jellysquid.mods.sodium.client.gl.buffer.GlBufferTexture;
import me.jellysquid.mods.sodium.client.gl.device.CommandList;
import net.caffeinemc.mods.sodium.api.buffer.UnmanagedBufferBuilder;

public class ParticleDataBuffer {
    private final GlBufferTexture bufferTexture;

    public ParticleDataBuffer(CommandList commandList) {
        this.bufferTexture = new GlBufferTexture(commandList);
    }

    public void uploadParticleData(CommandList commandList, UnmanagedBufferBuilder.Built data) {
        this.bufferTexture.putData(commandList, data.buffer, data.size);
    }

    public void bind() {
        this.bufferTexture.bind();
    }
}
