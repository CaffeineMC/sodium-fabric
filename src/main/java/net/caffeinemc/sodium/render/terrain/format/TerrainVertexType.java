package net.caffeinemc.sodium.render.terrain.format;

import net.caffeinemc.sodium.render.vertex.type.BlittableVertexType;
import net.caffeinemc.sodium.render.vertex.type.CustomVertexType;

public interface TerrainVertexType extends BlittableVertexType<TerrainVertexSink>, CustomVertexType<TerrainVertexSink, TerrainMeshAttribute> {
    /**
     * @return The scale to be applied to vertex coordinates
     */
    float getPositionScale();

    /**
     * @return The translation to be applied to vertex coordinates
     */
    float getPositionOffset();

    /**
     * @return The scale to be applied to texture coordinates
     */
    float getTextureScale();
}
