package me.jellysquid.mods.sodium.client.render.chunk.translucent_sorting.bsp_tree;

import java.nio.IntBuffer;

import org.joml.Vector3fc;

import me.jellysquid.mods.sodium.client.render.chunk.translucent_sorting.data.TranslucentData;

/**
 * Implementation note: Detecting and avoiding the double array when possible
 * brings no performance benefit in sorting speed, only a building speed
 * detriment.
 */
class InnerMultiPartitionBSPNode extends InnerPartitionBSPNode {
    private final float[] planeDistances; // one less than there are partitions

    private final BSPNode[] partitions;
    private final int[][] onPlaneQuads;

    InnerMultiPartitionBSPNode(float[] planeDistances, Vector3fc planeNormal,
            BSPNode[] partitions, int[][] onPlaneQuads) {
        super(planeNormal);
        this.planeDistances = planeDistances;
        this.partitions = partitions;
        this.onPlaneQuads = onPlaneQuads;
    }

    private void collectPlaneQuads(IntBuffer indexBuffer, int planeIndex) {
        if (this.onPlaneQuads[planeIndex] != null) {
            TranslucentData.writeQuadVertexIndexes(indexBuffer, this.onPlaneQuads[planeIndex]);
        }
    }

    private void collectPartitionQuads(IntBuffer indexBuffer, int partitionIndex, Vector3fc cameraPos) {
        if (this.partitions[partitionIndex] != null) {
            this.partitions[partitionIndex].collectSortedQuads(indexBuffer, cameraPos);
        }
    }

    @Override
    public void collectSortedQuads(IntBuffer indexBuffer, Vector3fc cameraPos) {
        // calculate the camera's distance. Then render the partitions in order of
        // distance to the partition the camera is in.
        var cameraDistance = this.planeNormal.dot(cameraPos);

        // forward sweep: collect quads until the camera is in the partition
        for (int i = 0; i < this.planeDistances.length; i++) {
            if (cameraDistance <= this.planeDistances[i]) {
                // collect the plane the camera is in
                var isOnPlane = cameraDistance == this.planeDistances[i];
                if (isOnPlane) {
                    this.collectPartitionQuads(indexBuffer, i, cameraPos);
                }

                // backwards sweep: collect all partitions backwards until the camera is reached
                for (int j = this.planeDistances.length; j > i; j--) {
                    this.collectPartitionQuads(indexBuffer, j, cameraPos);
                    this.collectPlaneQuads(indexBuffer, j - 1);
                }

                if (!isOnPlane) {
                    this.collectPartitionQuads(indexBuffer, i, cameraPos);
                }

                return;
            }

            // collect the quads in the partition and on the plane
            this.collectPartitionQuads(indexBuffer, i, cameraPos);
            this.collectPlaneQuads(indexBuffer, i);
        }

        // collect the last partition
        this.collectPartitionQuads(indexBuffer, this.planeDistances.length, cameraPos);
    }
}
