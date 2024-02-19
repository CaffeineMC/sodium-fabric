package net.caffeinemc.mods.sodium.client.render.chunk.translucent_sorting.bsp_tree;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntConsumer;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.objects.ReferenceArrayList;
import net.caffeinemc.mods.sodium.client.model.quad.properties.ModelQuadFacing;
import net.caffeinemc.mods.sodium.client.render.chunk.translucent_sorting.TQuad;
import net.caffeinemc.mods.sodium.client.render.chunk.translucent_sorting.data.TopoGraphSorting;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joml.Vector3fc;

import java.util.Arrays;

/**
 * Performs aligned BSP partitioning of many nodes and constructs appropriate
 * BSP nodes based on the result.
 * <p>
 * Implementation notes:
 * - Presorting the points in block-sized buckets doesn't help. It seems the
 * sort algorithm is just fast enough to handle this.
 * - Eliminating the use of partition objects doesn't help. Since there's
 * usually just very few partitions, it's not worth it, it seems.
 * - Using fastutil's LongArrays sorting options (radix and quicksort) is slower
 * than using Arrays.sort (which uses DualPivotQuicksort internally), even on
 * worlds with player-built structures.
 * - A simple attempt at lazily writing index data to the buffer didn't yield a
 * performance improvement. Maybe applying it to the multi partition node would
 * be more effective (but also much more complex and slower).
 * <p>
 * The encoding doesn't currently support negative distances (nor does such
 * support appear to be required). Their ordering is wrong when sorting them by
 * their binary representation. To fix this: "XOR all positive numbers with
 * 0x8000... and negative numbers with 0xffff... This should flip the sign bit
 * on both (so negative numbers go first), and then reverse the ordering on
 * negative numbers." from <a href="https://stackoverflow.com/q/43299299">StackOverflow</a>
 * <p>
 * When aligned partitioning fails the geometry is checked for intersection. If
 * there is intersection it means the section is unsortable and an approximation
 * is used instead. When it doesn't intersect but is not aligned partitionable,
 * it either requires unaligned partitioning (a hard problem not solved here)
 * or it's unpartitionable. It would be possible to insert a topo sorting node
 * here, but it's not worth the implementation effort unless it's found to be a
 * reasonable and common use case (I haven't been able to determine that it is).
 */
abstract class InnerPartitionBSPNode extends BSPNode {
    private static final int NODE_REUSE_THRESHOLD = 30;
    private static final int MAX_INTERSECTION_ATTEMPTS = 500;
    private static final int MAX_PRIMARY_INTERSECTOR_ITEMS = 40;
    private static final float PRIMARY_INTERSECTOR_THRESHOLD = 0.5f;

    private static final Logger LOGGER = LogManager.getLogger(InnerPartitionBSPNode.class);

    final Vector3fc planeNormal;
    final int axis;

    int[] indexMap;
    int fixedIndexOffset = BSPSortState.NO_FIXED_OFFSET;
    final NodeReuseData reuseData; // nullable

    /**
     * Stores data required for testing if the node can be re-used. This data is
     * only generated for select candidate nodes.
     * <p>
     * It only stores the set of indexes that this node was constructed from and
     * their extents since the BSP construction only cares about the "opaque" quad
     * geometry and not the normal or facing.
     * <p>
     * Since the indexes might be compressed, the count needs to be stored
     * separately from before compression.
     */
    record NodeReuseData(float[][] quadExtents, int[] indexes, int indexCount, int maxIndex) {
    }

    InnerPartitionBSPNode(NodeReuseData reuseData, int axis) {
        this.planeNormal = ModelQuadFacing.ALIGNED_NORMALS[axis];
        this.axis = axis;
        this.reuseData = reuseData;
    }

    abstract void addPartitionPlanes(BSPWorkspace workspace);

    static NodeReuseData prepareNodeReuse(BSPWorkspace workspace, IntArrayList indexes, int depth) {
        // if node reuse is enabled, only enable on the first level of children (not the
        // root node and not anything deeper than its children)
        if (workspace.prepareNodeReuse && depth == 1 && indexes.size() > NODE_REUSE_THRESHOLD) {
            // collect the extents of the indexed quads and hash them
            var quadExtents = new float[indexes.size()][];
            int maxIndex = -1;
            for (int i = 0; i < indexes.size(); i++) {
                var index = indexes.getInt(i);
                var quad = workspace.quads[index];
                var extents = quad.getExtents();
                quadExtents[i] = extents;
                maxIndex = Math.max(maxIndex, index);
            }

            // compress indexes but without sorting them, as the order needs to be the same
            // for the extents comparison loop to work
            return new NodeReuseData(
                    quadExtents,
                    BSPSortState.compressIndexes(indexes, false),
                    indexes.size(),
                    maxIndex);
        }
        return null;
    }

    private static class IndexRemapper implements IntConsumer {
        private final int[] indexMap;
        private final IntArrayList newIndexes;
        private int index = 0;
        private int firstOffset = 0;

        private static final int OFFSET_CHANGED = Integer.MIN_VALUE;

        IndexRemapper(int length, IntArrayList newIndexes) {
            this.indexMap = new int[length];
            this.newIndexes = newIndexes;
        }

        @Override
        public void accept(int oldIndex) {
            var newIndex = this.newIndexes.getInt(this.index);
            this.indexMap[oldIndex] = newIndex;
            var newOffset = newIndex - oldIndex;
            if (this.index == 0) {
                this.firstOffset = newOffset;
            } else if (this.firstOffset != newOffset) {
                this.firstOffset = OFFSET_CHANGED;
            }
            this.index++;
        }

        boolean hasFixedOffset() {
            return this.firstOffset != OFFSET_CHANGED;
        }
    }

    static InnerPartitionBSPNode attemptNodeReuse(BSPWorkspace workspace, IntArrayList newIndexes, InnerPartitionBSPNode oldNode) {
        if (oldNode == null) {
            return null;
        }

        oldNode.indexMap = null;
        oldNode.fixedIndexOffset = BSPSortState.NO_FIXED_OFFSET;

        var reuseData = oldNode.reuseData;
        if (reuseData == null) {
            return null;
        }

        var oldExtents = reuseData.quadExtents;
        if (oldExtents.length != newIndexes.size()) {
            return null;
        }

        for (int i = 0; i < newIndexes.size(); i++) {
            if (!workspace.quads[newIndexes.getInt(i)].extentsEqual(oldExtents[i])) {
                return null;
            }
        }

        // reuse old node and either apply a fixed offset or calculate an index map to
        // map from old to new indices
        var remapper = new IndexRemapper(reuseData.maxIndex + 1, newIndexes);
        BSPSortState.decompressOrRead(reuseData.indexes, remapper);

        // use a fixed offset if possible (if all old indices differ from the new ones
        // by the same amount)
        if (remapper.hasFixedOffset()) {
            oldNode.fixedIndexOffset = remapper.firstOffset;
        } else {
            oldNode.indexMap = remapper.indexMap;
        }

        // import the triggering data from the old node to ensure it still triggers at
        // the right time
        oldNode.addPartitionPlanes(workspace);

        return oldNode;
    }

    private static long encodeIntervalPoint(float distance, int quadIndex, int type) {
        return ((long) Float.floatToRawIntBits(distance) << 32) | ((long) type << 30) | quadIndex;
    }

    private static float decodeDistance(long encoded) {
        return Float.intBitsToFloat((int) (encoded >>> 32));
    }

    private static int decodeQuadIndex(long encoded) {
        return (int) (encoded & 0x3FFFFFFF);
    }

    private static int decodeType(long encoded) {
        return (int) (encoded >>> 30) & 0b11;
    }

    public static void validateQuadCount(int quadCount) {
        if (quadCount * 2 > 0x3FFFFFFF) {
            throw new IllegalArgumentException("Too many quads: " + quadCount);
        }
    }

    // the indices of the type are chosen such that tie-breaking items that have the
    // same distance with the type ascending yields a beneficial sort order
    // (END of the current interval, on-edge quads, then the START of the next
    // interval)

    // the start of a quad's extent in this direction
    private static final int INTERVAL_START = 2;

    // end end of a quad's extent in this direction
    private static final int INTERVAL_END = 0;

    // looking at a quad from the side where it has zero thickness
    private static final int INTERVAL_SIDE = 1;

    static BSPNode build(BSPWorkspace workspace, IntArrayList indexes, int depth, BSPNode oldNode) {
        // attempt reuse of the old node if possible
        if (oldNode instanceof InnerPartitionBSPNode oldInnerNode) {
            var reusedNode = InnerPartitionBSPNode.attemptNodeReuse(workspace, indexes, oldInnerNode);
            if (reusedNode != null) {
                return reusedNode;
            }
        }

        ReferenceArrayList<Partition> partitions = new ReferenceArrayList<>();
        LongArrayList points = new LongArrayList((int) (indexes.size() * 1.5));

        // find any aligned partition, search each axis
        for (int axisCount = 0; axisCount < 3; axisCount++) {
            int axis = (axisCount + depth + 1) % 3;
            var oppositeDirection = axis + 3;

            // collect all the geometry's start and end points in this direction
            points.clear();
            for (int quadIndex : indexes) {
                var quad = workspace.quads[quadIndex];
                var extents = quad.getExtents();
                var posExtent = extents[axis];
                var negExtent = extents[oppositeDirection];
                if (posExtent == negExtent) {
                    points.add(encodeIntervalPoint(posExtent, quadIndex, INTERVAL_SIDE));
                } else {
                    points.add(encodeIntervalPoint(posExtent, quadIndex, INTERVAL_END));
                    points.add(encodeIntervalPoint(negExtent, quadIndex, INTERVAL_START));
                }
            }

            // sort interval points by distance ascending and then by type. Sorting the
            // longs directly has the same effect because of the encoding.
            Arrays.sort(points.elements(), 0, points.size());

            // find gaps
            partitions.clear();
            float distance = -1;
            IntArrayList quadsBefore = null;
            IntArrayList quadsOn = null;
            int thickness = 0;
            for (long point : points) {
                switch (decodeType(point)) {
                    case INTERVAL_START -> {
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
                        }
                        if (quadsBefore == null) {
                            quadsBefore = new IntArrayList();
                        }
                        quadsBefore.add(decodeQuadIndex(point));
                    }
                    case INTERVAL_END -> {
                        thickness--;
                        if (quadsOn == null) {
                            distance = decodeDistance(point);
                        }
                    }
                    case INTERVAL_SIDE -> {
                        // if this point in a gap, it can be put on the plane itself
                        int pointQuadIndex = decodeQuadIndex(point);
                        if (thickness == 0) {
                            float pointDistance = decodeDistance(point);
                            if (quadsOn == null) {
                                // no partition end created yet, set here
                                quadsOn = new IntArrayList();
                                distance = pointDistance;
                            } else if (distance != pointDistance) {
                                // partition end has passed already, flush for new partition plane distance
                                partitions.add(new Partition(distance, quadsBefore, quadsOn));
                                distance = pointDistance;
                                quadsBefore = null;
                                quadsOn = new IntArrayList();
                            }
                            quadsOn.add(pointQuadIndex);
                        } else {
                            if (quadsBefore == null) {
                                throw new IllegalStateException("there must be started intervals here");
                            }
                            quadsBefore.add(pointQuadIndex);
                            if (quadsOn == null) {
                                distance = decodeDistance(point);
                            }
                        }
                    }
                }
            }

            // check a different axis if everything is in one quadsBefore,
            // which means there are no gaps
            if (quadsBefore != null && quadsBefore.size() == indexes.size()) {
                continue;
            }

            // check if there's a trailing plane. Otherwise, the last plane has distance -1
            // since it just holds the trailing quads
            boolean endsWithPlane = quadsOn != null;

            // flush the last partition, use the -1 distance to indicate the end if it
            // doesn't use quadsOn (which requires a certain distance to be given)
            if (quadsBefore != null || quadsOn != null) {
                partitions.add(new Partition(endsWithPlane ? distance : -1, quadsBefore, quadsOn));
            }

            // check if this can be turned into a binary partition node
            // (if there's at most two partitions and one plane)
            if (partitions.size() <= 2) {
                // get the two partitions
                var inside = partitions.get(0);
                var outside = partitions.size() == 2 ? partitions.get(1) : null;
                if (outside == null || !endsWithPlane) {
                    return InnerBinaryPartitionBSPNode.buildFromPartitions(workspace, indexes, depth, oldNode,
                            inside, outside, axis);
                }
            }

            // create a multi-partition node
            return InnerMultiPartitionBSPNode.buildFromPartitions(workspace, indexes, depth, oldNode,
                    partitions, axis, endsWithPlane);
        }

        var intersectingHandling = handleIntersecting(workspace, indexes, depth, oldNode);
        if (intersectingHandling != null) {
            return intersectingHandling;
        }

        // At this point we know the geometry is (probably) not intersecting, and it
        // can't be partitioned along an aligned axis. This means that either the
        // geometry is unpartitionable with free cuts (partitions that don't fragment
        // geometry) or it required unaligned partitioning. Unaligned partitioning is a
        // hard problem and solving it here isn't really necessary. Fully aligned
        // unpartitonable constructions exist but not in normal Minecraft and are also
        // not likely in any other normal scenario.

        // Throwing this exception will cause the entire section to be either topo
        // sorted or the distance sorting fallback to be used.
        // TODO: investigate BSP build failures if they happen, then remove this logging
        LOGGER.warn(
                "BSP build failure at {}. Please report this to douira for evaluation alongside with some way of reproducing the geometry in this section. (coordinates, and world file or seed)",
                workspace.sectionPos);

        // unpartitionable non-intersecting geometry is warned about, but then we attempt to topo sort it anyway
        var multiLeafNode = buildTopoMultiLeafNode(workspace, indexes);
        if (multiLeafNode == null) {
            throw new BSPBuildFailureException("No partition found but not intersecting and can't be statically topo sorted");
        }
        return multiLeafNode;
    }

    private static class QuadIndexConsumerIntoArray implements IntConsumer {
        final int[] indexes;
        private int index = 0;

        QuadIndexConsumerIntoArray(int size) {
            this.indexes = new int[size];
        }

        @Override
        public void accept(int value) {
            this.indexes[this.index++] = value;
        }
    }

    static private BSPNode handleIntersecting(BSPWorkspace workspace, IntArrayList indexes, int depth, BSPNode oldNode) {
        int testsRemaining = MAX_INTERSECTION_ATTEMPTS;

        boolean failedTopoSort = false;
        int[] intersectionCounts = null;
        IntOpenHashSet primaryIntersectorIndexes = null;
        int primaryIntersectorThreshold = 0;
        if (indexes.size() <= MAX_PRIMARY_INTERSECTOR_ITEMS) {
            intersectionCounts = new int[indexes.size()];
            primaryIntersectorThreshold = Math.max(2, (int) (indexes.size() * PRIMARY_INTERSECTOR_THRESHOLD));
        }

        for (int i = 0; i < indexes.size(); i++) {
            var quadA = workspace.quads[indexes.getInt(i)];

            for (int j = i + 1; j < indexes.size(); j++) {
                var quadB = workspace.quads[indexes.getInt(j)];

                // aligned quads intersect if their bounding boxes intersect
                if (TQuad.extentsIntersect(quadA, quadB)) {
                    if (intersectionCounts != null) {
                        var aCount = ++intersectionCounts[i];
                        if (aCount >= primaryIntersectorThreshold) {
                            if (primaryIntersectorIndexes == null) {
                                primaryIntersectorIndexes = new IntOpenHashSet(2);
                            }
                            primaryIntersectorIndexes.add(i);
                        }
                        var bCount = ++intersectionCounts[j];
                        if (bCount >= primaryIntersectorThreshold) {
                            if (primaryIntersectorIndexes == null) {
                                primaryIntersectorIndexes = new IntOpenHashSet(2);
                            }
                            primaryIntersectorIndexes.add(j);
                        }

                        // cancel primary intersector search if they all intersect with each other
                        if (primaryIntersectorIndexes != null && primaryIntersectorIndexes.size() == indexes.size()) {
                            primaryIntersectorIndexes = null;
                            intersectionCounts = null;
                        }
                    }

                    if (!failedTopoSort) {
                        var multiLeafNode = buildTopoMultiLeafNode(workspace, indexes);
                        if (multiLeafNode == null) {
                            if (intersectionCounts == null) {
                                throw new BSPBuildFailureException("Geometry is self-intersecting and can't be statically topo sorted");
                            }
                            failedTopoSort = true;
                        } else {
                            return multiLeafNode;
                        }
                    }
                }

                if (intersectionCounts == null && failedTopoSort) {
                    testsRemaining = 0;
                    break;
                }

                if (--testsRemaining <= 0) {
                    break;
                }
            }

            if (testsRemaining == 0) {
                break;
            }
        }

        if (primaryIntersectorIndexes == null) {
            return null;
        }

        // put the primary intersectors in a separate node that's always rendered last
        var nonPrimaryIntersectors = new IntArrayList(indexes.size() - primaryIntersectorIndexes.size());
        var primaryIntersectorQuadIndexes = new IntArrayList(primaryIntersectorIndexes.size());
        for (int i = 0; i < indexes.size(); i++) {
            if (primaryIntersectorIndexes.contains(i)) {
                primaryIntersectorQuadIndexes.add(indexes.getInt(i));
            } else {
                nonPrimaryIntersectors.add(indexes.getInt(i));
            }
        }
        return InnerFixedDoubleBSPNode.buildFromParts(workspace, indexes, depth, oldNode,
                nonPrimaryIntersectors, primaryIntersectorQuadIndexes);
    }

    static private BSPNode buildTopoMultiLeafNode(BSPWorkspace workspace, IntArrayList indexes) {
        var quadCount = indexes.size();
        var quads = new TQuad[quadCount];
        var activeToRealIndex = new int[quadCount];
        for (int i = 0; i < indexes.size(); i++) {
            var quadIndex = indexes.getInt(i);
            quads[i] = workspace.quads[quadIndex];
            activeToRealIndex[i] = quadIndex;
        }

        var indexWriter = new QuadIndexConsumerIntoArray(quadCount);
        if (!TopoGraphSorting.topoGraphSort(indexWriter, quads, quads.length, activeToRealIndex, null, null)) {
            return null;
        }

        return new LeafMultiBSPNode(BSPSortState.compressIndexesInPlace(indexWriter.indexes, false));
    }
}
