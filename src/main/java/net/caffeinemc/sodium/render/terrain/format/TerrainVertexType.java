package net.caffeinemc.sodium.render.terrain.format;

import net.caffeinemc.sodium.render.vertex.type.BlittableVertexType;
import net.caffeinemc.sodium.render.vertex.type.CustomVertexType;

public interface TerrainVertexType extends BlittableVertexType<TerrainVertexSink>, CustomVertexType<TerrainVertexSink, TerrainMeshAttribute> {
    float getVertexRange();
}
