package me.jellysquid.mods.sodium.client.render.chunk.gfni;

import java.nio.IntBuffer;

import org.joml.Vector3fc;

import it.unimi.dsi.fastutil.ints.IntArrays;
import it.unimi.dsi.fastutil.objects.Object2ReferenceOpenHashMap;
import me.jellysquid.mods.sodium.client.model.quad.properties.ModelQuadFacing;
import me.jellysquid.mods.sodium.client.util.collections.BitArray;
import me.jellysquid.mods.sodium.client.util.sorting.MergeSort;

public class ComplexSorting {
    private ComplexSorting() {
    }

    private static float halfspace(Vector3fc planeAnchor, Vector3fc planeNormal, Vector3fc point) {
        return (point.x() - planeAnchor.x()) * planeNormal.x() +
                (point.y() - planeAnchor.y()) * planeNormal.y() +
                (point.z() - planeAnchor.z()) * planeNormal.z();
    }

    /**
     * Test if the given point is within the half space defined by the plane anchor
     * and the plane normal. The normal points away from the space considered to be
     * inside.
     * 
     * @param planeAnchor the anchor of the plane
     * @param planeNormal the normal of the plane
     * @param point       the point to test
     */
    private static boolean pointOutsideHalfspace(Vector3fc planeAnchor, Vector3fc planeNormal, Vector3fc point) {
        return halfspace(planeAnchor, planeNormal, point) > 0;
    }

    private static boolean pointInsideHalfSpace(Vector3fc planeAnchor, Vector3fc planeNormal, Vector3fc point) {
        return halfspace(planeAnchor, planeNormal, point) < 0;
    }

    public static void distanceSortModified(IntBuffer indexBuffer, TQuad[] quads, Vector3fc cameraPos) {
        int[] indexes = new int[quads.length];
        BitArray visible = new BitArray(quads.length);
        float[] centerDist = new float[quads.length];
        for (int i = 0; i < indexes.length; i++) {
            indexes[i] = i;
            TQuad quad = quads[i];
            if (pointOutsideHalfspace(quad.center(), quad.normal(), cameraPos)) {
                visible.set(i);
                centerDist[i] = cameraPos.distanceSquared(quads[i].center());
            }
        }

        IntArrays.quickSort(indexes, (a, b) -> {
            // returning -1 results in the ordering a, b, returning 1 produces b, a

            boolean aVisible = visible.get(a);
            boolean bVisible = visible.get(b);

            // compare only pairs where both are visible
            if (aVisible && bVisible) {
                var quadA = quads[a];
                var quadB = quads[b];
                var normalA = quadA.normal();
                var normalB = quadB.normal();

                // this is a heuristic that attempts to reduce the sorting mistakes that happen
                // when two parallel planes made up of many quads are seen from different
                // angles. For quads with equal normals it sorts them only by their
                // normal-relative distance to the camera. This eliminates some of wrong sorting
                // that happens in tangent directions.
                if (normalA.equals(normalB)) {
                    var centerA = quadA.center();
                    var centerB = quadB.center();
                    var cameraDistance = normalB.dot(cameraPos);
                    var result = Float.compare(Math.abs(cameraDistance - normalB.dot(centerB)),
                            Math.abs(cameraDistance - normalB.dot(centerA)));
                    if (result != 0) {
                        return result;
                    }
                }

                return Float.compare(centerDist[b], centerDist[a]);

            }

            // put invisible quads last
            if (aVisible) {
                return -1;
            }
            if (bVisible) {
                return 1;
            }

            return 0;
        });

        TranslucentData.writeQuadVertexIndexes(indexBuffer, indexes);
    }

    private static int[] distanceSortIndexes(TQuad[] quads, Vector3fc cameraPos) {
        float[] keys = new float[quads.length];
        for (int i = 0; i < quads.length; i++) {
            keys[i] = cameraPos.distanceSquared(quads[i].center());
        }

        // TODO: use sort algorithm other than merge sort
        return MergeSort.mergeSort(keys);
    }

    public static void distanceSortDirect(IntBuffer indexBuffer, TQuad[] quads, Vector3fc cameraPos) {
        var indexes = distanceSortIndexes(quads, cameraPos);
        TranslucentData.writeQuadVertexIndexes(indexBuffer, indexes);
    }

    private static boolean orthoExtentWithinHalfspace(TQuad halfspace, TQuad otherQuad) {
        var halfspaceDirection = halfspace.facing().ordinal();
        var otherQuadOppositeDirection = halfspace.facing().getOpposite().ordinal();
        var sign = halfspace.facing().getSign();

        return sign * halfspace.extents()[halfspaceDirection] - QUERY_EPSILON > sign * otherQuad.extents()[otherQuadOppositeDirection];
    }

    private static boolean orthogonalQuadVisibleThrough(TQuad quad, TQuad otherQuad) {
        // A: test that the other quad has an extent within this quad's halfspace
        // B: test that this quad has an extent outside the other quad's halfspace
        return orthoExtentWithinHalfspace(quad, otherQuad) && !orthoExtentWithinHalfspace(otherQuad, quad);
    }

    /**
     * The index in each node's array in the graph where the number of outgoing
     * edges is stored.
     */
    private static final int OUTGOING_EDGES = ModelQuadFacing.DIRECTIONS;

    private static void makeEdge(int[][] graph, BitArray leafQuads,
            int fromQuadIndex, int toQuadIndex, int direction) {
        graph[toQuadIndex][direction] = fromQuadIndex;

        // increment outgoing edges and clear from the no outgoing edges set if it had
        // no outgoing edges beforehand
        if (graph[fromQuadIndex][OUTGOING_EDGES]++ == 0) {
            leafQuads.unset(fromQuadIndex);
        }
    }

    /**
     * This scanning topo sort algorithm assumes that a number of invariants about
     * the quads hold:
     * - quads are planar and actually have the normal of their facing
     * - quads don't intersect
     * - quads aren't shaped weirdly
     * - quads are axis-aligned
     * 
     * If a camera position is provided, the algorithm will only sort visible quads
     * and just render the other quads first without any particular ordering.
     * 
     * Sometimes this algorithm will fail even if a topological sort is possible due
     * to how it compresses the graph.
     * 
     * @param indexBuffer the buffer to write the topo sort result to
     * @param allQuads    the quads to sort
     * @param cameraPos   the camera position, or null if not visibility check
     *                    should be performed
     * @return true if the quads were sorted, false if there was a cycle
     */
    public static boolean topoSortAlignedScanningCyclic(IntBuffer indexBuffer, TQuad[] allQuads, Vector3fc cameraPos) {
        // if enabled, check for visibility and produce a mapping of indices
        TQuad[] quads = null;
        int[] activeToRealIndex = null;
        int activeQuads = 0;
        if (cameraPos != null) {
            // allocate the working quads and index map at the full size to avoid needing to
            // iterate the quads again after checking visibility
            quads = new TQuad[allQuads.length];
            activeToRealIndex = new int[allQuads.length];

            for (int i = 0; i < allQuads.length; i++) {
                TQuad quad = allQuads[i];
                if (pointOutsideHalfspace(quad.center(), quad.normal(), cameraPos)) {
                    activeToRealIndex[activeQuads] = i;
                    quads[activeQuads] = quad;
                    activeQuads++;
                } else {
                    // write the invisible quads right away
                    TranslucentData.putQuadVertexIndexes(indexBuffer, i);
                }
            }
        } else {
            quads = allQuads;
            activeQuads = allQuads.length;
        }

        /**
         * The translucent quad visibility graph is stored as an array for each quad.
         * Each quad's array stores the indexes of the quads that can see this quad
         * through them. The edges are stored backwards to avoid using dynamically
         * sized-lists for outgoing edges.
         * 
         * The last entry stores the number of *outgoing* edges.
         */
        int[][] graph = new int[activeQuads][ModelQuadFacing.DIRECTIONS + 1];

        // the set of quads that have no outgoing edges
        BitArray leafQuads = new BitArray(activeQuads);
        leafQuads.set(0, activeQuads);

        // a bitfield of the directions that have quads
        int activeDirections = 0;

        // initialize the graph with -1 to represent no edge
        for (int i = 0; i < activeQuads; i++) {
            activeDirections |= 1 << quads[i].facing().ordinal();

            for (int j = 0; j < ModelQuadFacing.DIRECTIONS; j++) {
                graph[i][j] = -1;
            }
        }

        // the stash of quads that have not yet been visible to the scanned quads
        BitArray stashedOrthoQuads = new BitArray(activeQuads);

        // keep around the allocation of the keys array
        float[] keys = new float[activeQuads];

        // to build the graph, perform scans for each direction
        for (int direction = 0; direction < ModelQuadFacing.DIRECTIONS; direction++) {
            // skip directions that have no quads
            if ((activeDirections & (1 << direction)) == 0) {
                continue;
            }

            ModelQuadFacing facing = ModelQuadFacing.VALUES[direction];
            ModelQuadFacing oppositeFacing = facing.getOpposite();
            int oppositeDirection = oppositeFacing.ordinal();
            int sign = facing.getSign();

            // generate keys for this direction
            for (int i = 0; i < activeQuads; i++) {
                // get the extent in the opposite direction of the scan because quads that are
                // visible from a scanning quad should be before it
                TQuad quad = quads[i];
                keys[i] = quad.extents()[oppositeDirection] * sign * -1;
            }

            int[] sortedQuads = MergeSort.mergeSort(keys);

            // the index of the last quad facing in scan direction (scanning quad) in the
            // sorted quad array
            int lastScanQuadPos = -1;

            // perform a scan by going through the sorted quads and making edges between the
            // scanning quads and the quads that precede them in the sort order
            stashedOrthoQuads.unset();
            for (int quadIndexPos = 0; quadIndexPos < activeQuads; quadIndexPos++) {
                int quadIndex = sortedQuads[quadIndexPos];
                TQuad quad = quads[quadIndex];
                if (quad.facing() != facing) {
                    continue;
                }

                // connect to the last scan quad if it exists
                if (lastScanQuadPos != -1) {
                    makeEdge(graph, leafQuads, quadIndex, sortedQuads[lastScanQuadPos], direction);
                }

                // check if any of the stashed quads are now visible
                for (int stashedQuadIndex = stashedOrthoQuads
                        .nextSetBit(0); stashedQuadIndex != -1; stashedQuadIndex = stashedOrthoQuads
                                .nextSetBit(stashedQuadIndex + 1)) {
                    TQuad stashedOrthoQuad = quads[stashedQuadIndex];

                    // if it's visible through the current quad, unstash and connect
                    if (orthogonalQuadVisibleThrough(quad, stashedOrthoQuad)) {
                        stashedOrthoQuads.unset(stashedQuadIndex);
                        makeEdge(graph, leafQuads, quadIndex, stashedQuadIndex, direction);
                    }
                }

                // check the quads facing in other directions than the scan facing.
                // initially increment to skip the last scan quad.
                for (++lastScanQuadPos; lastScanQuadPos < quadIndexPos; lastScanQuadPos++) {
                    int otherQuadIndex = sortedQuads[lastScanQuadPos];
                    TQuad otherQuad = quads[otherQuadIndex];

                    // discard quads that face in the opposite direction, they are never visible
                    if (otherQuad.facing() == oppositeFacing) {
                        continue;
                    }

                    // if it's visible through the current quad, add an edge.
                    // the quad has a direction that is orthogonal to the scan direction, since
                    // opposite quads were just ruled out and same facing quads are handled by the
                    // scan.
                    if (orthogonalQuadVisibleThrough(quad, otherQuad)) {
                        makeEdge(graph, leafQuads, quadIndex, otherQuadIndex, direction);
                    } else {
                        // otherwise stash it to check if it's visible by later quads in the scan
                        stashedOrthoQuads.set(otherQuadIndex);
                    }
                }

                // lastScanQuadPos is now at quadIndexPos
            }
        }

        // iterate through the set of quads with no outgoing edges until there are none
        // left to produce a topological sort of the graph
        for (int topoSortPos = 0; topoSortPos < activeQuads; topoSortPos++) {
            int nextLeafQuadIndex = leafQuads.nextSetBit(0);

            // if there are no leaf quads but not yet all quads have been processed,
            // there must be a cycle!
            if (nextLeafQuadIndex == -1) {
                return false;
            }

            leafQuads.unset(nextLeafQuadIndex);

            // add it to the topo sort result
            TranslucentData.putMappedQuadVertexIndexes(indexBuffer, nextLeafQuadIndex, activeToRealIndex);

            // remove the edges to this quad and mark them as leaves if that was the last
            // outgoing edge they had
            for (int direction = 0; direction < ModelQuadFacing.DIRECTIONS; direction++) {
                int otherQuadIndex = graph[nextLeafQuadIndex][direction];
                if (otherQuadIndex != -1 && --graph[otherQuadIndex][OUTGOING_EDGES] == 0) {
                    leafQuads.set(otherQuadIndex);
                }
            }
        }

        return true;
    }

    private static final float QUERY_EPSILON = 0.0011f;

    private static boolean testSeparatorRange(Object2ReferenceOpenHashMap<Vector3fc, double[]> distancesByNormal,
            Vector3fc normal, float start, float end) {
        var distances = distancesByNormal.get(normal);
        if (distances == null) {
            return false;
        }
        return Group.queryRange(distances, start - QUERY_EPSILON, end + QUERY_EPSILON);
    }

    private static boolean visibilityWithSeparator(TQuad quadA, TQuad quadB,
            Object2ReferenceOpenHashMap<Vector3fc, double[]> distancesByNormal, Vector3fc cameraPos) {
        // check if there is an aligned separator
        for (int direction = 0; direction < ModelQuadFacing.DIRECTIONS; direction++) {
            var facing = ModelQuadFacing.VALUES[direction];
            var oppositeFacing = facing.getOpposite();
            var oppositeDirection = oppositeFacing.ordinal();
            var sign = facing.getSign();

            // test that they're not overlapping in this direction
            var separatorRangeStart = sign * quadB.extents()[direction];
            var separatorRangeEnd = sign * quadA.extents()[oppositeDirection];
            if (separatorRangeStart > separatorRangeEnd) {
                continue;
            }

            // test that the camera doesn't exclude all separators
            var facingNormal = ModelQuadFacing.NORMALS[direction];
            var cameraDistance = facingNormal.dot(cameraPos);
            if (cameraDistance > separatorRangeEnd) {
                continue;
            }

            // restrict the separator range start to the camera distance
            separatorRangeStart = Math.max(separatorRangeStart, cameraDistance);

            // test if there is a separator plane that is outside/on the surface of the
            // current trigger volume
            if (testSeparatorRange(distancesByNormal, facingNormal,
                    separatorRangeStart, separatorRangeEnd)) {
                return false;
            }
        }

        // TODO: check all other normals too

        // visibility not disproven
        return true;
    }

    /**
     * Checks if one quad is visible through the other quad. This accepts arbitrary
     * quads, even unaligned ones.
     * 
     * @param quad              the quad through which the other quad is being
     *                          tested
     * @param other             the quad being tested
     * @param distancesByNormal a map of normals to sorted arrays of face plane
     *                          distances for disproving that the quads are visible
     *                          through eachother, null to disable
     * 
     * @return true if the other quad is visible through the first quad
     */
    private static boolean quadVisibleThrough(TQuad quad, TQuad other,
            Object2ReferenceOpenHashMap<Vector3fc, double[]> distancesByNormal, Vector3fc cameraPos) {
        if (quad == other) {
            return false;
        }

        // aligned quads
        var quadFacing = quad.facing();
        var otherFacing = other.facing();
        boolean result;
        if (quadFacing != ModelQuadFacing.UNASSIGNED && otherFacing != ModelQuadFacing.UNASSIGNED) {
            // opposites never see eachother
            if (quadFacing.getOpposite() == otherFacing) {
                return false;
            }

            // parallel quads, coplanar quads are not visible to eachother
            if (quadFacing == otherFacing) {
                var sign = quadFacing.getSign();
                var direction = quadFacing.ordinal();
                result = sign * quad.extents()[direction] > sign * other.extents()[direction];
            } else {
                // orthogonal quads
                result = orthogonalQuadVisibleThrough(quad, other);
            }
        } else {
            // at least one unaligned quad
            // this is an approximation since our quads don't store all their vertices.
            // check that other center is within the halfspace of quad and that quad isn't
            // in the halfspace of other
            result = pointInsideHalfSpace(quad.center(), quad.normal(), other.center())
                    && !pointInsideHalfSpace(other.center(), other.normal(), quad.center());
        }

        // if enabled and necessary, try to disprove this see-through relationship with
        // a separator plane
        if (result && distancesByNormal != null) {
            return visibilityWithSeparator(quad, other, distancesByNormal, cameraPos);
        }

        return result;
    }

    /**
     * Performs a topological sort but constructs the full forward graph without
     * using a compression technique like
     * {@link #topoSortAlignedScanningCyclic(IntBuffer, TQuad[], Vector3fc)} does.
     * Only
     * sort visible quads if a camera position is provided.
     * 
     * @param indexBuffer       the buffer to write the topo sort result to
     * @param allQuads          the quads to sort
     * @param distancesByNormal a map of normals to sorted arrays of face plane
     *                          distances, null to disable
     * @param cameraPos         the camera position, or null to disable the
     *                          visibility check
     * @return true if the quads were sorted, false if there was a cycle
     */
    public static boolean topoSortLinkedListAcyclic(
            IntBuffer indexBuffer, TQuad[] allQuads,
            Object2ReferenceOpenHashMap<Vector3fc, double[]> distancesByNormal,
            Vector3fc cameraPos) {
        // TODO: quads visibility filter copy-pasted from topoSortAlignedAcyclic
        // if enabled, check for visibility and produce a mapping of indices
        TQuad[] quads = null;
        int[] activeToRealIndex = null;
        int activeQuads = 0;
        if (cameraPos != null) {
            // allocate the working quads and index map at the full size to avoid needing to
            // iterate the quads again after checking visibility
            quads = new TQuad[allQuads.length];
            activeToRealIndex = new int[allQuads.length];

            for (int i = 0; i < allQuads.length; i++) {
                TQuad quad = allQuads[i];
                if (pointOutsideHalfspace(quad.center(), quad.normal(), cameraPos)) {
                    activeToRealIndex[activeQuads] = i;
                    quads[activeQuads] = quad;
                    activeQuads++;
                } else {
                    // write the invisible quads right away
                    TranslucentData.putQuadVertexIndexes(indexBuffer, i);
                }
            }
        } else {
            quads = allQuads;
            activeQuads = allQuads.length;
        }

        // special case for 0 or 1 quads because the algorithm below doesn't work for
        // those cases (and it's just faster to skip it)
        if (activeQuads == 0) {
            return true;
        }
        if (activeQuads == 1) {
            TranslucentData.putMappedQuadVertexIndexes(indexBuffer, 0, activeToRealIndex);
            return true;
        }

        // special case 2 quads for performance
        if (activeQuads == 2) {
            var a = 0;
            var b = 1;
            if (quadVisibleThrough(quads[a], quads[b], null, null)) {
                a = 1;
                b = 0;
            }
            TranslucentData.putMappedQuadVertexIndexes(indexBuffer, a, activeToRealIndex);
            TranslucentData.putMappedQuadVertexIndexes(indexBuffer, b, activeToRealIndex);
            return true;
        }

        // int-based doubly linked list of active quad indexes
        // TODO: put this linked list in a class
        // TODO: sorting is sometimes wrong, see example in sandstone world
        int[] next = new int[activeQuads];
        int[] prev = new int[activeQuads];
        int start = 0;
        for (int i = 0; i < activeQuads - 1; i++) {
            next[i] = i + 1;
            prev[i + 1] = i;
        }
        next[activeQuads - 1] = -1;
        prev[0] = -1;

        // go through all the active quads and insert them into the list at the point
        // where they are after all the quads that are visible through them
        for (int quadIndex = 0; quadIndex < activeQuads; quadIndex++) {
            TQuad quad = quads[quadIndex];

            // test all other quads
            int insertAfter = -1;
            int otherQuadIndex = start;
            while (otherQuadIndex != -1) {
                TQuad otherQuad = quads[otherQuadIndex];
                if (quadIndex != otherQuadIndex
                        && quadVisibleThrough(quad, otherQuad, distancesByNormal, cameraPos)) {
                    insertAfter = otherQuadIndex;
                }
                otherQuadIndex = next[otherQuadIndex];
            }

            // if the move index is lower than insert after, instead move the insert after
            // to be right before the move index
            int removeIndex = quadIndex;
            boolean swapped = quadIndex < insertAfter; // implies insertAfter != -1
            if (swapped) {
                removeIndex = insertAfter;
            }

            // remove quadIndex or insertAfter from current location
            int currentNext = next[removeIndex];
            int currentPrev = prev[removeIndex];
            if (currentNext != -1) {
                prev[currentNext] = currentPrev;
            }
            if (currentPrev != -1) {
                next[currentPrev] = currentNext;
            } else {
                start = currentNext;
            }

            // insert after the identified last quad that this quad needs to be after
            if (insertAfter == -1) {
                // insert quadIndex at the start
                next[quadIndex] = start;
                prev[quadIndex] = -1;
                prev[start] = quadIndex;
                start = quadIndex;
            } else if (swapped) {
                // insert insertAfter at the start if quadIndex is the start
                if (start == quadIndex) {
                    next[insertAfter] = start;
                    prev[insertAfter] = -1;
                    prev[start] = insertAfter;
                    start = insertAfter;
                } else {
                    // insert insertAfter before quadIndex
                    next[insertAfter] = quadIndex;
                    next[prev[quadIndex]] = insertAfter;
                    prev[insertAfter] = prev[quadIndex];
                    prev[quadIndex] = insertAfter;
                }
            } else {
                // insert quadIndex after insertAfter
                prev[quadIndex] = insertAfter;
                if (next[insertAfter] != -1) { // end of chain
                    prev[next[insertAfter]] = quadIndex;
                }
                next[quadIndex] = next[insertAfter];
                next[insertAfter] = quadIndex;
            }
        }

        // write the sorted quads to the buffer
        for (int i = 0; i < activeQuads; i++) {
            TranslucentData.putMappedQuadVertexIndexes(indexBuffer, start, activeToRealIndex);
            start = next[start];
        }

        // detect cycles
        if (start != -1) {
            return false;
        }

        return true;
    }

    /**
     * Performs a topological sort without constructing the full graph in memory by
     * doing a DFS on the implicit graph. Edges are tested as they are searched for
     * and if necessary separator planes are used to disprove visibility.
     * 
     * TODO: the issue with it sometimes failing to sort is probably from the
     * separator mechanism not working for unaligned quads yet.
     * 
     * @param indexBuffer       the buffer to write the topo sort result to
     * @param allQuads          the quads to sort
     * @param distancesByNormal a map of normals to sorted arrays of face plane
     *                          distances, null to disable
     * @param cameraPos         the camera position, or null to disable the
     *                          visibility check
     */
    public static boolean topoSortDepthFirstCyclic(
            IntBuffer indexBuffer, TQuad[] allQuads,
            Object2ReferenceOpenHashMap<Vector3fc, double[]> distancesByNormal,
            Vector3fc cameraPos) {
        // TODO: quads visibility filter copy-pasted from topoSortAlignedAcyclic
        // if enabled, check for visibility and produce a mapping of indices
        TQuad[] quads = null;
        int[] activeToRealIndex = null;
        int activeQuads = 0;
        if (cameraPos != null) {
            // allocate the working quads and index map at the full size to avoid needing to
            // iterate the quads again after checking visibility
            quads = new TQuad[allQuads.length];
            activeToRealIndex = new int[allQuads.length];

            for (int i = 0; i < allQuads.length; i++) {
                TQuad quad = allQuads[i];
                // TODO: this could introduce wrong sorting if the real normal and the quantized
                // normal don't line up, ignoring the quad if it's not visible through the
                // quantized one but visible in-camera
                if (pointOutsideHalfspace(quad.center(), quad.normal(), cameraPos)) {
                    activeToRealIndex[activeQuads] = i;
                    quads[activeQuads] = quad;
                    activeQuads++;
                } else {
                    // write the invisible quads right away
                    TranslucentData.putQuadVertexIndexes(indexBuffer, i);
                }
            }
        } else {
            quads = allQuads;
            activeQuads = allQuads.length;
        }

        // special case for 0 to 2 quads
        if (activeQuads == 0) {
            return true;
        }
        if (activeQuads == 1) {
            TranslucentData.putMappedQuadVertexIndexes(indexBuffer, 0, activeToRealIndex);
            return true;
        }

        // special case 2 quads for performance
        if (activeQuads == 2) {
            var a = 0;
            var b = 1;
            if (quadVisibleThrough(quads[a], quads[b], null, null)) {
                a = 1;
                b = 0;
            }
            TranslucentData.putMappedQuadVertexIndexes(indexBuffer, a, activeToRealIndex);
            TranslucentData.putMappedQuadVertexIndexes(indexBuffer, b, activeToRealIndex);
            return true;
        }

        BitArray unvisited = new BitArray(activeQuads);
        unvisited.set(0, activeQuads);
        int visitedCount = 0;
        BitArray onStack = new BitArray(activeQuads);
        int[] stack = new int[activeQuads];
        int[] nextEdge = new int[activeQuads];

        // start dfs searches until all quads are visited
        while (visitedCount < activeQuads) {
            int stackPos = 0;
            var root = unvisited.nextSetBit(0);
            stack[stackPos] = root;
            onStack.set(root);
            nextEdge[stackPos] = 0;

            while (stackPos >= 0) {
                // start at next edge and find an unvisited quad
                var currentQuadIndex = stack[stackPos];
                var nextEdgeTest = unvisited.nextSetBit(nextEdge[stackPos]);
                if (nextEdgeTest != -1) {
                    var currentQuad = quads[currentQuadIndex];
                    var nextQuad = quads[nextEdgeTest];
                    if (quadVisibleThrough(currentQuad, nextQuad, distancesByNormal, cameraPos)) {
                        // if the visible quad is on the stack, there is a cycle
                        if (onStack.getAndSet(nextEdgeTest)) {
                            return false;
                        }

                        // set the next edge
                        nextEdge[stackPos] = nextEdgeTest + 1;

                        // visit the next quad, onStack is already set
                        stackPos++;
                        stack[stackPos] = nextEdgeTest;
                        nextEdge[stackPos] = 0;
                        continue;
                    } else {
                        // go to the next edge
                        nextEdgeTest++;

                        // if we haven't reached the end of the edges yet
                        if (nextEdgeTest < activeQuads) {
                            nextEdge[stackPos] = nextEdgeTest;
                            continue;
                        }
                    }
                }

                // no more edges left, pop the stack
                onStack.unset(currentQuadIndex);
                visitedCount++;
                unvisited.unset(currentQuadIndex);
                stackPos--;

                // write to the index buffer since the order is now correct
                TranslucentData.putMappedQuadVertexIndexes(indexBuffer, currentQuadIndex, activeToRealIndex);
            }
        }

        return true;
    }
}
