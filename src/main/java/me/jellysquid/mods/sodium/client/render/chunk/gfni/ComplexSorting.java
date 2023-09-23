package me.jellysquid.mods.sodium.client.render.chunk.gfni;

import java.nio.IntBuffer;

import org.joml.Vector3fc;

import it.unimi.dsi.fastutil.ints.IntArrays;
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

        TranslucentData.writeVertexIndexes(indexBuffer, indexes);
    }

    private static boolean extentWithinHalfspace(TQuad halfspace, TQuad otherQuad) {
        var halfspaceDirection = halfspace.facing().ordinal();
        var otherQuadOppositeDirection = halfspace.facing().getOpposite().ordinal();
        var sign = halfspace.facing().getSign();

        return sign * halfspace.extents()[halfspaceDirection] > sign * otherQuad.extents()[otherQuadOppositeDirection];
    }

    private static boolean orthogonalQuadVisibleThrough(TQuad quad, TQuad otherQuad) {
        // A: test that the other quad has an extent within this quad's halfspace
        // B: test that this quad has an extent outside the other quad's halfspace
        return extentWithinHalfspace(quad, otherQuad) && !extentWithinHalfspace(otherQuad, quad);
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

    public static void topoSortAlignedAcyclicSafe(IntBuffer indexBuffer, TQuad[] allQuads, Vector3fc cameraPos) {
        if (!topoSortAlignedAcyclic(indexBuffer, allQuads, cameraPos)) {
            throw new IllegalStateException("Cyclic quad visibility graph!");
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
     * @param indexBuffer the buffer to write the topo sort result to
     * @param quads       the quads to sort
     * @param cameraPos   the camera position, or null if not visibility check
     *                    should be performed
     * @return true if the quads were sorted, false if there was a cycle
     */
    public static boolean topoSortAlignedAcyclic(IntBuffer indexBuffer, TQuad[] allQuads, Vector3fc cameraPos) {
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

        // initialize the graph with -1 to represent no edge
        for (int i = 0; i < activeQuads; i++) {
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
            if (cameraPos != null) {
                TranslucentData.putQuadVertexIndexes(indexBuffer, activeToRealIndex[nextLeafQuadIndex]);
            } else {
                TranslucentData.putQuadVertexIndexes(indexBuffer, nextLeafQuadIndex);
            }

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
}
