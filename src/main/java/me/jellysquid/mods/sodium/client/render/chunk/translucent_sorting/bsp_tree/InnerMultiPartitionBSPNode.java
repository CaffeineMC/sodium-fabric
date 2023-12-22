package me.jellysquid.mods.sodium.client.render.chunk.translucent_sorting.bsp_tree;

import org.joml.Vector3fc;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.objects.ReferenceArrayList;

/**
 * Partitions quads into multiple child BSP nodes with multiple parallel
 * partition planes. This is uses less memory and time than constructing a
 * binary BSP tree through more partitioning passes.
 * 
 * Implementation note: Detecting and avoiding the double array when possible
 * brings no performance benefit in sorting speed, only a building speed
 * detriment.
 */
class InnerMultiPartitionBSPNode extends InnerPartitionBSPNode {
    private final float[] planeDistances; // one less than there are partitions

    private final BSPNode[] partitions;
    private final int[][] onPlaneQuads;

    InnerMultiPartitionBSPNode(NodeReuseData reuseData, int axis, float[] planeDistances,
            BSPNode[] partitions, int[][] onPlaneQuads) {
        super(reuseData, axis);
        this.planeDistances = planeDistances;
        this.partitions = partitions;
        this.onPlaneQuads = onPlaneQuads;
    }

    @Override
    void addPartitionPlanes(BSPWorkspace workspace) {
        for (int i = 0; i < this.planeDistances.length; i++) {
            workspace.addAlignedPartitionPlane(this.axis, this.planeDistances[i]);
        }

        // recurse on children to also add their planes
        for (var partition : this.partitions) {
            if (partition instanceof InnerPartitionBSPNode inner) {
                inner.addPartitionPlanes(workspace);
            }
        }
    }

    private void collectPlaneQuads(BSPSortState sortState, int planeIndex) {
        if (this.onPlaneQuads[planeIndex] != null) {
            sortState.writeIndexes(this.onPlaneQuads[planeIndex]);
        }
    }

    private void collectPartitionQuads(BSPSortState sortState, int partitionIndex, Vector3fc cameraPos) {
        if (this.partitions[partitionIndex] != null) {
            this.partitions[partitionIndex].collectSortedQuads(sortState, cameraPos);
        }
    }

    @Override
    void collectSortedQuads(BSPSortState sortState, Vector3fc cameraPos) {
        sortState.startNode(this);

        // calculate the camera's distance. Then render the partitions in order of
        // distance to the partition the camera is in.
        var cameraDistance = this.planeNormal.dot(cameraPos);

        // forward sweep: collect quads until the camera is in the partition
        for (int i = 0; i < this.planeDistances.length; i++) {
            if (cameraDistance <= this.planeDistances[i]) {
                // collect the plane the camera is in
                var isOnPlane = cameraDistance == this.planeDistances[i];
                if (isOnPlane) {
                    this.collectPartitionQuads(sortState, i, cameraPos);
                }

                // backwards sweep: collect all partitions backwards until the camera is reached
                for (int j = this.planeDistances.length; j > i; j--) {
                    this.collectPartitionQuads(sortState, j, cameraPos);
                    this.collectPlaneQuads(sortState, j - 1);
                }

                if (!isOnPlane) {
                    this.collectPartitionQuads(sortState, i, cameraPos);
                }

                return;
            }

            // collect the quads in the partition and on the plane
            this.collectPartitionQuads(sortState, i, cameraPos);
            this.collectPlaneQuads(sortState, i);
        }

        // collect the last partition
        this.collectPartitionQuads(sortState, this.planeDistances.length, cameraPos);
    }

    static BSPNode buildFromPartitions(BSPWorkspace workspace, IntArrayList indexes, int depth, BSPNode oldNode,
            ReferenceArrayList<Partition> partitions, int axis, boolean endsWithPlane) {
        int planeCount = endsWithPlane ? partitions.size() : partitions.size() - 1;
        float[] planeDistances = new float[planeCount];
        BSPNode[] partitionNodes = new BSPNode[planeCount + 1];
        int[][] onPlaneQuads = new int[planeCount][];

        BSPNode[] oldPartitionNodes = null;
        float[] oldPlaneDistances = null;
        int oldChildIndex = 0;
        float oldPartitionDistance = 0;
        if (oldNode instanceof InnerMultiPartitionBSPNode multiNode
                && multiNode.axis == axis && multiNode.partitions.length > 0) {
            oldPartitionNodes = multiNode.partitions;
            oldPlaneDistances = multiNode.planeDistances;
            oldPartitionDistance = multiNode.planeDistances[0];
        }

        // write the partition planes and nodes
        for (int i = 0, count = partitions.size(); i < count; i++) {
            var partition = partitions.get(i);

            // if the partition actually has a plane
            float partitionDistance = -1;
            if (endsWithPlane || i < count - 1) {
                partitionDistance = partition.distance();
                workspace.addAlignedPartitionPlane(axis, partitionDistance);

                // NOTE: sanity check
                if (partitionDistance == -1) {
                    throw new IllegalStateException("partition distance not set");
                }

                planeDistances[i] = partitionDistance;
            }

            if (partition.quadsBefore() != null) {
                BSPNode oldChild = null;

                if (oldPartitionNodes != null) {
                    // if there's a node that matches the partition's distance, use it as the old
                    // node. Search forwards through the old plane distances to find a candidate
                    while (oldChildIndex < oldPartitionNodes.length && oldPartitionDistance < partitionDistance) {
                        oldChildIndex++;
                        oldPartitionDistance = oldChildIndex < oldPlaneDistances.length
                                ? oldPlaneDistances[oldChildIndex]
                                : -1;
                    }
                    if (oldChildIndex < oldPartitionNodes.length && oldPartitionDistance == partitionDistance) {
                        oldChild = oldPartitionNodes[oldChildIndex];
                    }
                }

                partitionNodes[i] = BSPNode.build(workspace, partition.quadsBefore(), depth, oldChild);
            }
            if (partition.quadsOn() != null) {
                onPlaneQuads[i] = BSPSortState.compressIndexes(partition.quadsOn());
            }
        }

        return new InnerMultiPartitionBSPNode(prepareNodeReuse(workspace, indexes, depth),
                axis, planeDistances, partitionNodes, onPlaneQuads);
    }
}
