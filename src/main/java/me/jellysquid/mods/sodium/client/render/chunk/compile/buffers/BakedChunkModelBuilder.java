package me.jellysquid.mods.sodium.client.render.chunk.compile.buffers;

import me.jellysquid.mods.sodium.client.model.quad.properties.ModelQuadFacing;
import me.jellysquid.mods.sodium.client.render.chunk.vertex.builder.ChunkMeshBufferBuilder;
import me.jellysquid.mods.sodium.client.render.chunk.data.BuiltSectionInfo;
import net.minecraft.client.texture.Sprite;

public class BakedChunkModelBuilder implements ChunkModelBuilder {
    private final ChunkMeshBufferBuilder[] meshBuffers;

    private BuiltSectionInfo.Builder renderData;

    public BakedChunkModelBuilder(ChunkMeshBufferBuilder[] meshBuffers) {
        this.meshBuffers = meshBuffers;
    }

    @Override
    public ChunkMeshBufferBuilder getMeshBuffer(ModelQuadFacing facing) {
        return this.meshBuffers[facing.ordinal()];
    }

    @Override
    public void addSprite(Sprite sprite) {
        this.renderData.addSprite(sprite);
    }

    public void destroy() {
        for (ChunkMeshBufferBuilder builder : this.meshBuffers) {
            builder.destroy();
        }
    }

    public void begin(BuiltSectionInfo.Builder renderData, int sectionIndex) {
        this.renderData = renderData;

        for (var vertexBuffer : this.meshBuffers) {
            vertexBuffer.start(sectionIndex);
        }
    }
}
