package me.jellysquid.mods.sodium.client.render.chunk.compile.buffers;

import me.jellysquid.mods.sodium.client.model.quad.properties.ModelQuadFacing;
import me.jellysquid.mods.sodium.client.render.chunk.terrain.material.Material;
import me.jellysquid.mods.sodium.client.render.chunk.vertex.builder.ChunkMeshBufferBuilder;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.texture.Sprite;

public interface ChunkModelBuilder {
    ChunkMeshBufferBuilder getVertexBuffer(ModelQuadFacing facing);

    void addSprite(Sprite sprite);

    VertexConsumer asVertexConsumer(Material material);
}
