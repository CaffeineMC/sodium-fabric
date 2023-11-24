package me.jellysquid.mods.sodium.client.render.chunk.translucent_sorting.bsp_tree;

import java.util.Comparator;

import org.joml.Vector3fc;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.objects.ReferenceArrayList;
import me.jellysquid.mods.sodium.client.model.quad.properties.ModelQuadFacing;
import me.jellysquid.mods.sodium.client.render.chunk.translucent_sorting.TopoGraphSorting;

/**
 * TODO:
 * - make partition finding more sophisticated by using distancesByNormal
 * and other information.
 * - pass partition information to child nodes instead of discarding if
 * partition didn't work
 * - detect many coplanar quads and avoid partitioning sorting alltogether. Try
 * to partition so that large groups of coplanar quads are kept together and
 * then not sorted at all.
 * - sort the quads inside a node using the same heuristics as in the global
 * level: convex hulls (like cuboids where the faces point outwards) don't need
 * to be sorted. This could be used to optimize the inside of honey blocks, and
 * other blocks.
 */
abstract class InnerPartitionBSPNode extends BSPNode {
    final Vector3fc planeNormal;

    InnerPartitionBSPNode(Vector3fc planeNormal) {
        this.planeNormal = planeNormal;
    }

    private static record IntervalPoint(float distance, int quadIndex, byte type) {
        // the indices of the type are chosen such that tie-breaking items that have the
        // same distance with the type ascending yields a beneficial sort order
        // (END of the current interval, on-edge quads, then the START of the next
        // interval)

        // the start of a quad's extent in this direction
        static final byte START = 2;

        // end end of a quad's extent in this direction
        static final byte END = 0;

        // looking at a quad from the side where it has zero thickness
        static final byte SIDE = 1;
    }

    private static final Comparator<InnerPartitionBSPNode.IntervalPoint> INTERVAL_POINT_COMPARATOR = (a, b) -> {
        // sort by distance ascending
        float distanceDiff = a.distance() - b.distance();
        if (distanceDiff != 0) {
            return distanceDiff < 0 ? -1 : 1;
        }

        // sort by type ascending
        return a.type() - b.type();
    };

    /**
     * models a partition of the space into a set of quads that lie inside or on the
     * plane with the specified distance. If the distance is -1 this is the "end"
     * partition after the last partition plane.
     */
    private static record Partition(float distance, IntArrayList quadsBefore, IntArrayList quadsOn) {
    }

    static BSPNode build(BSPWorkspace workspace) {
        // special case two quads
        if (workspace.indexes.size() == 2) {
            var quadA = workspace.quads[workspace.indexes.getInt(0)];
            var quadB = workspace.quads[workspace.indexes.getInt(1)];

            // check for coplanar or mutually invisible quads
            var facingA = quadA.facing();
            var facingB = quadB.facing();
            var normalA = quadA.normal();
            var normalB = quadB.normal();
            if (
            // coplanar quads
            ((facingA == ModelQuadFacing.UNASSIGNED || facingB == ModelQuadFacing.UNASSIGNED)
                    ? // opposite normal (distance irrelevant)
                    normalA.x() == -normalB.x()
                            && normalA.y() == -normalB.y()
                            && normalA.z() == -normalB.z()
                            // same normal and same distance
                            || normalA.equals(quadB.normal())
                                    && normalA.dot(quadA.center()) == quadB.normal().dot(quadB.center())
                    // aligned same distance
                    : quadA.extents()[facingA.ordinal()] == quadB.extents()[facingB.ordinal()])
                    // facing away from eachother
                    || facingA == facingB.getOpposite()
                    // otherwise mutually invisible
                    || facingA != ModelQuadFacing.UNASSIGNED
                            && facingB != ModelQuadFacing.UNASSIGNED
                            && !TopoGraphSorting.orthogonalQuadVisibleThrough(quadA, quadB)
                            && !TopoGraphSorting.orthogonalQuadVisibleThrough(quadB, quadA)) {
                return new LeafMultiBSPNode(workspace.indexes.toIntArray());
            }
        }

        ReferenceArrayList<InnerPartitionBSPNode.Partition> partitions = new ReferenceArrayList<>();
        ReferenceArrayList<InnerPartitionBSPNode.IntervalPoint> points = new ReferenceArrayList<>(
                (int) (workspace.indexes.size() * 1.5));

        // find any aligned partition, search each axis
        for (int axis = 0; axis < 3; axis++) {
            var facing = ModelQuadFacing.VALUES[axis];
            var oppositeFacing = facing.getOpposite();
            var oppositeDirection = oppositeFacing.ordinal();

            // collect all the geometry's start and end points in this direction
            points.clear();
            for (int quadIndex : workspace.indexes) {
                var quad = workspace.quads[quadIndex];
                var extents = quad.extents();
                var posExtent = extents[axis];
                var negExtent = extents[oppositeDirection];
                if (posExtent == negExtent) {
                    points.add(new IntervalPoint(posExtent, quadIndex, IntervalPoint.SIDE));
                } else {
                    points.add(new IntervalPoint(posExtent, quadIndex, IntervalPoint.END));
                    points.add(new IntervalPoint(negExtent, quadIndex, IntervalPoint.START));
                }
            }

            points.sort(INTERVAL_POINT_COMPARATOR);

            // find gaps
            partitions.clear();
            float distance = -1;
            IntArrayList quadsBefore = null;
            IntArrayList quadsOn = null;
            int thickness = 0;
            for (InnerPartitionBSPNode.IntervalPoint point : points) {
                switch (point.type()) {
                    case IntervalPoint.START -> {
                        // unless at the start, flush if there's a gap
                        if (thickness == 0 && (quadsBefore != null || quadsOn != null)) {
                            partitions.add(new Partition(distance, quadsBefore, quadsOn));
                            distance = -1;
                            quadsBefore = null;
                            quadsOn = null;
                        }

                        thickness++;

                        // flush to partition if still writing last partition
                        if (quadsOn != null) {
                            if (distance == -1) {
                                throw new IllegalStateException("distance not set");
                            }
                            partitions.add(new Partition(distance, quadsBefore, quadsOn));
                            distance = -1;
                            quadsOn = null;
                            quadsOn = null;
                        }
                        if (quadsBefore == null) {
                            quadsBefore = new IntArrayList();
                        }
                        quadsBefore.add(point.quadIndex());
                    }
                    case IntervalPoint.END -> {
                        thickness--;
                        if (quadsOn == null) {
                            distance = point.distance();
                        }
                    }
                    case IntervalPoint.SIDE -> {
                        // if this point in a gap, it can be put on the plane itself
                        if (thickness == 0) {
                            if (quadsOn == null) {
                                // no partition end created yet, set here
                                quadsOn = new IntArrayList();
                                distance = point.distance();
                            } else if (distance != point.distance()) {
                                // partition end has passed already, flush for new partition plane distance
                                partitions.add(new Partition(distance, quadsBefore, quadsOn));
                                distance = point.distance();
                                quadsBefore = null;
                                quadsOn = new IntArrayList();
                            }
                            quadsOn.add(point.quadIndex());
                        } else {
                            if (quadsBefore == null) {
                                throw new IllegalStateException("there must be started intervals here");
                            }
                            quadsBefore.add(point.quadIndex());
                            if (quadsOn == null) {
                                distance = point.distance();
                            }
                        }
                    }
                }
            }

            // check a different axis if everything is in one quadsBefore,
            // which means there are no gaps
            if (quadsBefore != null && quadsBefore.size() == workspace.indexes.size()) {
                // TODO: reuse the sorted point array for child nodes (add to workspace)
                continue;
            }

            // check if there's a trailing plane. Otherwise the last plane has distance -1
            // since it just holds the trailing quads
            boolean trailingPlane = quadsOn != null;

            // flush the last partition, use the -1 distance to indicate the end if it
            // doesn't use quadsOn (which requires a certain distance to be given)
            if (quadsBefore != null || quadsOn != null) {
                partitions.add(new Partition(trailingPlane ? distance : -1, quadsBefore, quadsOn));
            }

            // check if this can be turned into a binary partition node
            // (if there's at most two partitions and one plane)
            if (partitions.size() <= 2) {
                // get the two partitions
                var inside = partitions.get(0);
                var outside = partitions.size() == 2 ? partitions.get(1) : null;
                if (outside == null || !trailingPlane) {
                    var partitionDistance = inside.distance();
                    workspace.addAlignedPartitionPlane(axis, partitionDistance);

                    BSPNode insideNode = null;
                    BSPNode outsideNode = null;
                    if (inside.quadsBefore() != null) {
                        workspace.indexes = inside.quadsBefore();
                        insideNode = BSPNode.build(workspace);
                    }
                    if (outside != null) {
                        workspace.indexes = outside.quadsBefore();
                        outsideNode = BSPNode.build(workspace);
                    }
                    var onPlane = inside.quadsOn() == null ? null : inside.quadsOn().toIntArray();

                    return new InnerBinaryPartitionBSPNode(
                            partitionDistance, ModelQuadFacing.NORMALS[axis],
                            insideNode, outsideNode, onPlane);
                }
            }

            // create a multi-partition node
            int planeCount = trailingPlane ? partitions.size() : partitions.size() - 1;
            float[] planeDistances = new float[planeCount];
            BSPNode[] partitionNodes = new BSPNode[planeCount + 1];
            int[][] onPlaneQuads = new int[planeCount][];

            // write the partition planes and nodes
            for (int i = 0, count = partitions.size(); i < count; i++) {
                var partition = partitions.get(i);

                // if the partition actually has a plane
                if (trailingPlane || i < count - 1) {
                    var partitionDistance = partition.distance();
                    workspace.addAlignedPartitionPlane(axis, partitionDistance);

                    // TODO: remove
                    if (partitionDistance == -1) {
                        throw new IllegalStateException("partition distance not set");
                    }

                    planeDistances[i] = partitionDistance;
                }

                if (partition.quadsBefore() != null) {
                    workspace.indexes = partition.quadsBefore();
                    partitionNodes[i] = BSPNode.build(workspace);
                }
                if (partition.quadsOn() != null) {
                    onPlaneQuads[i] = partition.quadsOn().toIntArray();
                }
            }

            return new InnerMultiPartitionBSPNode(planeDistances, ModelQuadFacing.NORMALS[axis],
                    partitionNodes, onPlaneQuads);
        }

        // TODO: attempt unaligned partitioning, convex decomposition, etc.

        throw new BSPBuildFailureException("no partition found");
    }
}