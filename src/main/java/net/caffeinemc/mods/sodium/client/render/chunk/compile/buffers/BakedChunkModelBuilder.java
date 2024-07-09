package net.caffeinemc.mods.sodium.client.render.chunk.compile.buffers;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.caffeinemc.mods.sodium.client.model.quad.properties.ModelQuadFacing;
import net.caffeinemc.mods.sodium.client.render.chunk.data.BuiltSectionInfo;
import net.caffeinemc.mods.sodium.client.render.chunk.terrain.material.Material;
import net.caffeinemc.mods.sodium.client.render.chunk.translucent_sorting.TranslucentGeometryCollector;
import net.caffeinemc.mods.sodium.client.render.chunk.vertex.builder.ChunkMeshBufferBuilder;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;

public class BakedChunkModelBuilder implements ChunkModelBuilder {
    private final ChunkMeshBufferBuilder[] vertexBuffers;
    private final ChunkVertexConsumer fallbackVertexConsumer = new ChunkVertexConsumer(this);

    private BuiltSectionInfo.Builder renderData;

    public BakedChunkModelBuilder(ChunkMeshBufferBuilder[] vertexBuffers) {
        this.vertexBuffers = vertexBuffers;
    }

    @Override
    public ChunkMeshBufferBuilder getVertexBuffer(ModelQuadFacing facing) {
        return this.vertexBuffers[facing.ordinal()];
    }

    @Override
    public void addSprite(TextureAtlasSprite sprite) {
        this.renderData.addSprite(sprite);
    }

    @Override
    public VertexConsumer asFallbackVertexConsumer(Material material, TranslucentGeometryCollector collector) {
        fallbackVertexConsumer.setData(material, collector);
        return fallbackVertexConsumer;
    }

    public void destroy() {
        for (ChunkMeshBufferBuilder builder : this.vertexBuffers) {
            builder.destroy();
        }
    }

    public void begin(BuiltSectionInfo.Builder renderData, int sectionIndex) {
        this.renderData = renderData;

        for (var vertexBuffer : this.vertexBuffers) {
            vertexBuffer.start(sectionIndex);
        }
    }
}
