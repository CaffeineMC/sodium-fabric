package me.jellysquid.mods.sodium.client.render.chunk.data;

import org.joml.Vector3f;

import me.jellysquid.mods.sodium.client.model.quad.properties.ModelQuadFacing;
import me.jellysquid.mods.sodium.client.render.chunk.vertex.format.ChunkVertexType;
import me.jellysquid.mods.sodium.client.util.sorting.VertexSorters;

public class DynamicTranslucentData implements TranslucentData {
    private IndexedPrimitives data;

    public DynamicTranslucentData(ChunkVertexType vertexType, ChunkMeshData meshData) {
        var buffer = meshData.getVertexData().getDirectBuffer();
        int primitiveCount = meshData.getVertexCount() / 4; // TODO: is it quads or triangles?
        var vertexRange = meshData.getPart(ModelQuadFacing.UNASSIGNED);
        this.data = new IndexedPrimitives(vertexType, buffer, vertexRange, primitiveCount);
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
