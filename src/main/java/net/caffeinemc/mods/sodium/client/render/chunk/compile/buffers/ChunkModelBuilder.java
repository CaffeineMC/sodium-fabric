package net.caffeinemc.mods.sodium.client.render.chunk.compile.buffers;

import net.caffeinemc.mods.sodium.client.model.quad.properties.ModelQuadFacing;
import net.caffeinemc.mods.sodium.client.render.chunk.vertex.builder.ChunkMeshBufferBuilder;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;

public interface ChunkModelBuilder {
    ChunkMeshBufferBuilder getVertexBuffer(ModelQuadFacing facing);

    void addSprite(TextureAtlasSprite sprite);
}
