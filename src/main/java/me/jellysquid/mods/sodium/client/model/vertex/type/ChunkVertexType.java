package me.jellysquid.mods.sodium.client.model.vertex.type;

import me.jellysquid.mods.sodium.client.render.chunk.format.ChunkMeshAttribute;
import me.jellysquid.mods.sodium.client.render.chunk.format.ModelVertexSink;

public interface ChunkVertexType extends BlittableVertexType<ModelVertexSink>, CustomVertexType<ModelVertexSink, ChunkMeshAttribute> {
    /**
     * @return The scale to be applied to vertex coordinates
     */
    float getModelScale();

    /**
     * @return The scale to be applied to texture coordinates
     */
    float getTextureScale();
}
