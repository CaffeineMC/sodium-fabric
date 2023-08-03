package me.jellysquid.mods.sodium.client.render.chunk.data;

import org.joml.Vector3f;

import it.unimi.dsi.fastutil.objects.ReferenceArrayList;
import me.jellysquid.mods.sodium.client.model.quad.properties.ModelQuadFacing;
import me.jellysquid.mods.sodium.client.util.sorting.VertexSorters;

public class DynamicTranslucentData implements TranslucentData {
    private IndexedPrimitives data;

    public DynamicTranslucentData(ReferenceArrayList<Vector3f>[] centers) {
        var length = 0;
        for (int i = 0; i < ModelQuadFacing.COUNT; i++) {
            var centersForFacing = centers[i];
            if (centersForFacing != null) {
                length += centersForFacing.size();
            }
        }
        var centersArray = new Vector3f[length];
        int index = 0;
        for (int i = 0; i < ModelQuadFacing.COUNT; i++) {
            var centersForFacing = centers[i];
            if (centersForFacing != null) {
                for (var center : centersForFacing) {
                    centersArray[index++] = center;
                }
            }
        }
        this.data = new IndexedPrimitives(centersArray);
    }

    @Override
    public Vector3f[] getCenters(ModelQuadFacing facing) {
        if (facing != ModelQuadFacing.UNASSIGNED) {
            throw new UnsupportedOperationException("Cannot get centers for facing other than UNASSIGNED");
        }
        return this.data.centers;
    }

    @Override
    public int[] getIndexes(ModelQuadFacing facing) {
        if (facing != ModelQuadFacing.UNASSIGNED) {
            throw new UnsupportedOperationException("Cannot get indexes for facing other than UNASSIGNED");
        }
        return this.data.indexes;
    }

    @Override
    public boolean isDynamic() {
        return true;
    }

    @Override
    public void sort(Vector3f cameraPos) {
        this.data.indexes = VertexSorters.sortByDistance(cameraPos).sort(this.data.centers);
    }
}
