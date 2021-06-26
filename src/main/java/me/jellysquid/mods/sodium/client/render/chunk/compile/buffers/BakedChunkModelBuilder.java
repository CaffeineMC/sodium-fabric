package me.jellysquid.mods.sodium.client.render.chunk.compile.buffers;

import me.jellysquid.mods.sodium.client.model.PrimitiveSink;
import me.jellysquid.mods.sodium.client.model.quad.properties.ModelQuadFacing;
import me.jellysquid.mods.sodium.client.render.chunk.data.ChunkRenderData;
import me.jellysquid.mods.sodium.client.render.chunk.format.ModelVertexSink;
import net.minecraft.client.texture.Sprite;
import net.minecraft.util.math.Vec3i;

public class BakedChunkModelBuilder implements ChunkModelBuilder {
    private final PrimitiveSink<ModelVertexSink>[] builders;
    private final ChunkRenderData.Builder renderData;

    public BakedChunkModelBuilder(PrimitiveSink<ModelVertexSink>[] builders,
                                  ChunkRenderData.Builder renderData) {
        this.builders = builders;
        this.renderData = renderData;
    }

    @Override
    public PrimitiveSink<ModelVertexSink> getBuilder(ModelQuadFacing facing) {
        return this.builders[facing.ordinal()];
    }

    @Override
    public void addSprite(Sprite sprite) {
        this.renderData.addSprite(sprite);
    }
}
