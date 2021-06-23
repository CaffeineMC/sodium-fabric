package me.jellysquid.mods.sodium.client.render.chunk.compile.buffers;

import me.jellysquid.mods.sodium.client.model.PrimitiveSink;
import me.jellysquid.mods.sodium.client.model.quad.properties.ModelQuadFacing;
import me.jellysquid.mods.sodium.client.render.chunk.format.ModelVertexSink;
import net.minecraft.client.texture.Sprite;

public interface ChunkModelBuilder {
    PrimitiveSink<ModelVertexSink> getBuilder(ModelQuadFacing facing);

    void addSprite(Sprite sprite);

    int getOffset();
}
