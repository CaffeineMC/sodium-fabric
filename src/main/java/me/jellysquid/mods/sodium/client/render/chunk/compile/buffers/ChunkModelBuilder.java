package me.jellysquid.mods.sodium.client.render.chunk.compile.buffers;

import me.jellysquid.mods.sodium.client.render.chunk.vertex.builder.ChunkMeshBufferBuilder;
import net.minecraft.client.texture.Sprite;

public interface ChunkModelBuilder {
    ChunkMeshBufferBuilder getVertexBuffer(int facing);

    void addSprite(Sprite sprite);
}
