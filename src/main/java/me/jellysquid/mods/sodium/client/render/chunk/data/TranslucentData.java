package me.jellysquid.mods.sodium.client.render.chunk.data;

import org.joml.Vector3f;

import it.unimi.dsi.fastutil.objects.ReferenceArrayList;
import me.jellysquid.mods.sodium.client.model.quad.properties.ModelQuadFacing;
import me.jellysquid.mods.sodium.client.render.chunk.gfni.SortType;

/**
 * TODO: figure out if the encoded vertex data should be decoded or taken from
 * the BlockRenderer directly without decoding the buffers. It would be less
 * decoding work, but it would also be unnecessary in some cases where GFNI
 * later decides that it's not necessary.
 */
public interface TranslucentData {
    public static TranslucentData fromMeshData(SortType sortType, ReferenceArrayList<Vector3f>[] centers) {
        // TODO: handle new sort types
        switch (sortType) {
            case NONE:
                return null;
            case STATIC_NORMAL_RELATIVE:
                return new StaticTranslucentData(centers);
            case DYNAMIC_ALL:
                return new DynamicTranslucentData(centers);
            default:
                throw new UnsupportedOperationException("Unhandled sort type: " + sortType);
        }
    }

    public Vector3f[] getCenters(ModelQuadFacing facing);

    public int[] getIndexes(ModelQuadFacing facing);

    public boolean isDynamic();

    public void sort(Vector3f cameraPos);
}
