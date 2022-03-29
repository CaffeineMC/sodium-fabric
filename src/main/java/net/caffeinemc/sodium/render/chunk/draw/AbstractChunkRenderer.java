package net.caffeinemc.sodium.render.chunk.draw;

import net.caffeinemc.gfx.api.device.RenderDevice;
import net.caffeinemc.gfx.api.texture.Sampler;
import net.caffeinemc.sodium.render.terrain.format.TerrainVertexType;
import org.lwjgl.opengl.GL33C;

public abstract class AbstractChunkRenderer implements ChunkRenderer {
    protected final TerrainVertexType vertexType;

    protected final RenderDevice device;

    protected final Sampler blockTextureSampler;
    protected final Sampler blockTextureMippedSampler;

    protected final Sampler lightTextureSampler;

    public AbstractChunkRenderer(RenderDevice device, TerrainVertexType vertexType) {
        this.device = device;
        this.vertexType = vertexType;

        this.blockTextureSampler = device.createSampler();
        this.blockTextureSampler.setParameter(GL33C.GL_TEXTURE_MIN_FILTER, GL33C.GL_NEAREST);
        this.blockTextureSampler.setParameter(GL33C.GL_TEXTURE_MAG_FILTER, GL33C.GL_NEAREST);

        this.blockTextureMippedSampler = device.createSampler();
        this.blockTextureMippedSampler.setParameter(GL33C.GL_TEXTURE_MIN_FILTER, GL33C.GL_NEAREST_MIPMAP_LINEAR);
        this.blockTextureMippedSampler.setParameter(GL33C.GL_TEXTURE_MAG_FILTER, GL33C.GL_NEAREST);

        this.lightTextureSampler = device.createSampler();
        this.lightTextureSampler.setParameter(GL33C.GL_TEXTURE_MIN_FILTER, GL33C.GL_LINEAR);
        this.lightTextureSampler.setParameter(GL33C.GL_TEXTURE_MAG_FILTER, GL33C.GL_LINEAR);
        this.lightTextureSampler.setParameter(GL33C.GL_TEXTURE_WRAP_S, GL33C.GL_CLAMP_TO_EDGE);
        this.lightTextureSampler.setParameter(GL33C.GL_TEXTURE_WRAP_T, GL33C.GL_CLAMP_TO_EDGE);
    }

    @Override
    public void delete() {
        this.device.deleteSampler(this.blockTextureSampler);
        this.device.deleteSampler(this.blockTextureMippedSampler);
        this.device.deleteSampler(this.lightTextureSampler);
    }
}
