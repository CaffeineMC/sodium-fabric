package net.caffeinemc.sodium.render.chunk.draw;

import net.caffeinemc.gfx.api.device.RenderDevice;
import net.caffeinemc.gfx.api.texture.Sampler;
import net.caffeinemc.gfx.api.texture.parameters.AddressMode;
import net.caffeinemc.gfx.api.texture.parameters.FilterMode;
import net.caffeinemc.gfx.api.texture.parameters.MipmapMode;

public abstract class AbstractChunkRenderer implements ChunkRenderer {
    protected final RenderDevice device;

    protected final Sampler blockTextureSampler;
    protected final Sampler blockTextureMippedSampler;

    protected final Sampler lightTextureSampler;

    public AbstractChunkRenderer(RenderDevice device) {
        this.device = device;

        this.blockTextureSampler = device.createSampler(
                FilterMode.NEAREST,
                null,
                FilterMode.NEAREST,
                null,
                null,
                null
        );

        this.blockTextureMippedSampler = device.createSampler(
                FilterMode.NEAREST,
                MipmapMode.LINEAR,
                FilterMode.NEAREST,
                null,
                null,
                null
        );

        this.lightTextureSampler = device.createSampler(
                FilterMode.LINEAR,
                null,
                FilterMode.LINEAR,
                AddressMode.CLAMP_TO_EDGE,
                AddressMode.CLAMP_TO_EDGE,
                null
        );
    }

    @Override
    public void delete() {
        this.device.deleteSampler(this.blockTextureSampler);
        this.device.deleteSampler(this.blockTextureMippedSampler);
        this.device.deleteSampler(this.lightTextureSampler);
    }

    public enum BufferTarget {
        VERTICES
    }
}
