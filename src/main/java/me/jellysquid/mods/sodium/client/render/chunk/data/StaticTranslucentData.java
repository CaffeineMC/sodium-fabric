package me.jellysquid.mods.sodium.client.render.chunk.data;

import org.joml.Vector3f;

import com.mojang.blaze3d.systems.VertexSorter;

import it.unimi.dsi.fastutil.objects.ReferenceArrayList;
import me.jellysquid.mods.sodium.client.model.quad.properties.ModelQuadFacing;
import me.jellysquid.mods.sodium.client.util.sorting.VertexSorters;

public class StaticTranslucentData implements TranslucentData {
    private static final VertexSorter[] SORTERS = new VertexSorter[ModelQuadFacing.DIRECTIONS];

    static {
        for (int i = 0; i < ModelQuadFacing.DIRECTIONS; i++) {
            SORTERS[i] = VertexSorters.sortByAxis(ModelQuadFacing.VALUES[i]);
        }
    }

    private final IndexedPrimitives[] data = new IndexedPrimitives[ModelQuadFacing.DIRECTIONS];

    public StaticTranslucentData(ReferenceArrayList<Vector3f>[] centers) {
        for (int i = 0; i < ModelQuadFacing.DIRECTIONS; i++) {
            var centersForFacing = centers[i];
            if (centersForFacing != null) {
                this.data[i] = new IndexedPrimitives((Vector3f[]) centersForFacing.toArray());
            }
        }
    }

    @Override
    public Vector3f[] getCenters(ModelQuadFacing facing) {
        if (facing == ModelQuadFacing.UNASSIGNED) {
            throw new UnsupportedOperationException("Cannot get centers for UNASSIGNED facing");
        }
        return this.data[facing.ordinal()].centers;
    }

    @Override
    public int[] getIndexes(ModelQuadFacing facing) {
        if (facing == ModelQuadFacing.UNASSIGNED) {
            throw new UnsupportedOperationException("Cannot get indexes for UNASSIGNED facing");
        }
        return this.data[facing.ordinal()].indexes;
    }

    @Override
    public boolean isDynamic() {
        return false;
    }

    @Override
    public void sort(Vector3f cameraPos) {
        for (int i = 0; i < ModelQuadFacing.DIRECTIONS; i++) {
            var dataForFacing = this.data[i];
            if (dataForFacing != null) {
                dataForFacing.indexes = SORTERS[i].sort(dataForFacing.centers);
            }
        }
    }
}
