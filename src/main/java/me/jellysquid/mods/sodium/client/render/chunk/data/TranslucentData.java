package me.jellysquid.mods.sodium.client.render.chunk.data;

import org.joml.Vector3f;

import me.jellysquid.mods.sodium.client.model.quad.properties.ModelQuadFacing;
import me.jellysquid.mods.sodium.client.render.chunk.gfni.SortType;
import me.jellysquid.mods.sodium.client.render.chunk.vertex.format.ChunkVertexType;

/**
 * TODO: figure out if the encoded vertex data should be decoded or taken from
 * the BlockRenderer directly without decoding the buffers. It would be less
 * decoding work, but it would also be unnecessary in some cases where GFNI
 * later decides that it's not necessary.
 */
public interface TranslucentData {
    public static TranslucentData fromMeshData(ChunkVertexType vertexType, ChunkMeshData meshData, SortType sortType) {
        switch (sortType) {
            case NONE:
                return null;
            case STATIC_NORMAL_RELATIVE:
                return new StaticTranslucentData(vertexType, meshData);
            case DYNAMIC:
                return new DynamicTranslucentData(vertexType, meshData);
            default:
                throw new UnsupportedOperationException("Unknown sort type: " + sortType);
        }
    }

    public Vector3f[] getCenters(ModelQuadFacing facing);

    public int[] getIndexes(ModelQuadFacing facing);

    public boolean isDynamic();

    public void sort(Vector3f cameraPos);
}
