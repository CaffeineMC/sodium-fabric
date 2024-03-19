package me.jellysquid.mods.sodium.client.render.particle;

import me.jellysquid.mods.sodium.client.gl.arena.staging.StagingBufferBuilder;
import me.jellysquid.mods.sodium.client.gl.buffer.GlBufferTexture;
import me.jellysquid.mods.sodium.client.gl.buffer.GlContinuousUploadBuffer;
import me.jellysquid.mods.sodium.client.gl.buffer.GlMutableBuffer;
import me.jellysquid.mods.sodium.client.gl.device.CommandList;
import net.caffeinemc.mods.sodium.api.buffer.UnmanagedBufferBuilder;

public class ParticleBuffers {
    private final GlMutableBuffer particleData;
    private final GlContinuousUploadBuffer textureCache;

    private final GlBufferTexture particleDataTex;
    private final GlBufferTexture textureCacheTex;

    public ParticleBuffers(CommandList commandList) {
        this.particleData = commandList.createMutableBuffer();
        this.textureCache = new GlContinuousUploadBuffer(commandList);

        this.particleDataTex = new GlBufferTexture(particleData, 3);
        this.textureCacheTex = new GlBufferTexture(textureCache, 4);
    }

    public void uploadParticleData(CommandList commandList, StagingBufferBuilder data, UnmanagedBufferBuilder.Built cache) {
        data.endAndUpload(commandList, this.particleData, 0);
        this.textureCache.uploadOverwrite(commandList, cache.buffer, cache.size);
    }

    public void bind() {
        this.particleDataTex.bind();
        this.textureCacheTex.bind();
    }
}
