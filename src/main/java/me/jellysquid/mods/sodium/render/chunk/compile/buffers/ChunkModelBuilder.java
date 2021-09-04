package me.jellysquid.mods.sodium.render.chunk.compile.buffers;

import me.jellysquid.mods.sodium.model.IndexBufferBuilder;
import me.jellysquid.mods.sodium.model.quad.properties.ModelQuadFacing;
import me.jellysquid.mods.sodium.render.chunk.format.ModelVertexSink;
import net.minecraft.client.texture.Sprite;

public interface ChunkModelBuilder {
    ModelVertexSink getVertexSink();

    IndexBufferBuilder getIndexSink(ModelQuadFacing facing);

    @Deprecated
    void addSprite(Sprite sprite);
}
