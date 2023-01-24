package me.jellysquid.mods.sodium.client.render.chunk.compile.buffers;

import me.jellysquid.mods.sodium.client.model.IndexBufferBuilder;
import me.jellysquid.mods.sodium.client.model.quad.properties.ModelQuadFacing;
import me.jellysquid.mods.sodium.client.render.vertex.type.ChunkVertexBufferBuilder;
import net.minecraft.client.texture.Sprite;

public interface ChunkModelBuilder {
    ChunkVertexBufferBuilder getVertexBuffer();

    IndexBufferBuilder getIndexBuffer(ModelQuadFacing facing);

    void addSprite(Sprite sprite);

}
