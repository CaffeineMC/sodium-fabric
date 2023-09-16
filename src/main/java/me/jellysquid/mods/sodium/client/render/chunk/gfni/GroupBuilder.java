package me.jellysquid.mods.sodium.client.render.chunk.gfni;

import java.nio.IntBuffer;

import org.joml.Vector3f;
import org.joml.Vector3fc;

import it.unimi.dsi.fastutil.ints.Int2ReferenceLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.ReferenceArrayList;
import me.jellysquid.mods.sodium.client.SodiumClientMod;
import me.jellysquid.mods.sodium.client.gl.util.VertexRange;
import me.jellysquid.mods.sodium.client.gui.SodiumGameOptions.SortBehavior;
import me.jellysquid.mods.sodium.client.model.quad.ModelQuadView;
import me.jellysquid.mods.sodium.client.model.quad.properties.ModelQuadFacing;
import me.jellysquid.mods.sodium.client.render.chunk.data.BuiltSectionMeshParts;
import me.jellysquid.mods.sodium.client.render.chunk.vertex.format.ChunkVertexEncoder;
import me.jellysquid.mods.sodium.client.util.NativeBuffer;
import me.jellysquid.mods.sodium.client.util.collections.BitArray;
import me.jellysquid.mods.sodium.client.util.sorting.MergeSort;
import net.minecraft.util.math.ChunkSectionPos;

/**
 * The group builder collects the data from the renderers and builds data
 * structures for either dynamic triggering or static sorting. It determines the
 * best sort type for the section and performs translucency visibility graph
 * sorting with a topological sort algorithm if necessary.
 * 
 * TODO: can use a bunch more optimizations, this is a prototype.
 * TODO list:
 * - use continuous arrays for the quad centers and quad storage
 * - use more accurate normals for unaligned topo sort? do we do topo sort on
 * unaligned faces at all?
 * - bail early during rendering if we decide not to topo sort, then we don't
 * need to gather all of the data
 * - do normal-relative sorting here too? or keep it in StaticTranslucentData?
 * - detail how the graph construction and the topo sort works in the GFNI doc
 * - make the resulting static index buffer actually available to the renderer
 */
public class GroupBuilder {
    // TODO: debugging
    private static int topoSortHits = 0;
    private static int cyclicGraphHits = 0;
    private static int unalignedDynamicHits = 0;

    public static final Vector3fc[] ALIGNED_NORMALS = new Vector3fc[ModelQuadFacing.DIRECTIONS];

    private static final int OPPOSING_X = 1 << ModelQuadFacing.POS_X.ordinal() | 1 << ModelQuadFacing.NEG_X.ordinal();
    private static final int OPPOSING_Y = 1 << ModelQuadFacing.POS_Y.ordinal() | 1 << ModelQuadFacing.NEG_Y.ordinal();
    private static final int OPPOSING_Z = 1 << ModelQuadFacing.POS_Z.ordinal() | 1 << ModelQuadFacing.NEG_Z.ordinal();

    static {
        for (int i = 0; i < ModelQuadFacing.DIRECTIONS; i++) {
            ALIGNED_NORMALS[i] = new Vector3f(ModelQuadFacing.VALUES[i].toDirection().getUnitVector());
        }
    }

    AccumulationGroup[] axisAlignedDistances;
    Int2ReferenceLinkedOpenHashMap<AccumulationGroup> unalignedDistances;

    final ChunkSectionPos sectionPos;
    private int facePlaneCount = 0;
    private int alignedNormalBitmap = 0;
    private Vector3f minBounds = new Vector3f(16, 16, 16);
    private Vector3f maxBounds = new Vector3f(0, 0, 0);

    private int unalignedQuadCount = 0;

    /**
     * List of translucent quads being rendered.
     */
    public final ReferenceArrayList<Quad> quads = new ReferenceArrayList<>();

    public SortType sortType;

    public GroupBuilder(ChunkSectionPos sectionPos) {
        this.sectionPos = sectionPos;
    }

    record Quad(ModelQuadFacing facing, Vector3fc normal, Vector3f center, float[] extents) {
    }

    public void appendQuad(ModelQuadView quadView, ChunkVertexEncoder.Vertex[] vertices, ModelQuadFacing facing) {
        float xSum = 0;
        float ySum = 0;
        float zSum = 0;

        // initialize extents to positive or negative infinity
        // POS_X, POS_Y, POS_Z, NEG_X, NEG_Y, NEG_Z
        float[] extents = new float[] {
                Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY,
                Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY };

        for (int i = 0; i < 4; i++) {
            var x = vertices[i].x;
            var y = vertices[i].y;
            var z = vertices[i].z;

            if (facing != ModelQuadFacing.UNASSIGNED && this.unalignedDistances == null) {
                minBounds.x = Math.min(minBounds.x, x);
                minBounds.y = Math.min(minBounds.y, y);
                minBounds.z = Math.min(minBounds.z, z);

                maxBounds.x = Math.max(maxBounds.x, x);
                maxBounds.y = Math.max(maxBounds.y, y);
                maxBounds.z = Math.max(maxBounds.z, z);
            }

            extents[0] = Math.max(extents[0], x);
            extents[1] = Math.max(extents[1], y);
            extents[2] = Math.max(extents[2], z);
            extents[3] = Math.min(extents[3], x);
            extents[4] = Math.min(extents[4], y);
            extents[5] = Math.min(extents[5], z);

            // TODO: can we also just use two vertices at opposite corners of the quad?
            xSum += x;
            ySum += y;
            zSum += z;
        }

        var center = new Vector3f(xSum * 0.25f, ySum * 0.25f, zSum * 0.25f);

        // TODO: some of these things should probably only be computed on demand, and an
        // allocation of a Quad object should be avoided
        AccumulationGroup accGroup;
        if (facing == ModelQuadFacing.UNASSIGNED) {
            int normalX = quadView.getGFNINormX();
            int normalY = quadView.getGFNINormY();
            int normalZ = quadView.getGFNINormZ();

            if (this.unalignedDistances == null) {
                this.unalignedDistances = new Int2ReferenceLinkedOpenHashMap<>(4);
            }

            // the key for the hash map is the normal packed into an int
            // the lowest byte is 0xFF to prevent collisions with axis-aligned normals
            // (assuming quantization with 32, which is 5 bits per component)
            int normalKey = 0xFF | (normalX & 0xFF << 8) | (normalY & 0xFF << 15) | (normalZ & 0xFF << 22);
            accGroup = this.unalignedDistances.get(normalKey);

            if (accGroup == null) {
                // actually normalize the vector to ensure it's a unit vector
                // for the rest of the process which requires that
                Vector3f normal = new Vector3f(normalX, normalY, normalZ);
                normal.normalize();
                accGroup = new AccumulationGroup(sectionPos, normal, normalKey);
                this.unalignedDistances.put(normalKey, accGroup);
            }
            this.quads.add(new Quad(facing, accGroup.normal, center, extents));
            this.unalignedQuadCount++;
        } else {
            if (this.axisAlignedDistances == null) {
                this.axisAlignedDistances = new AccumulationGroup[ModelQuadFacing.DIRECTIONS];
            }

            int quadDirection = facing.ordinal();
            accGroup = this.axisAlignedDistances[quadDirection];

            if (accGroup == null) {
                accGroup = new AccumulationGroup(sectionPos, ALIGNED_NORMALS[quadDirection], quadDirection);
                this.axisAlignedDistances[quadDirection] = accGroup;
                this.alignedNormalBitmap |= 1 << quadDirection;
            }

            this.quads.add(new Quad(facing, accGroup.normal, center, extents));
        }

        var firstVertex = vertices[0];
        if (accGroup.addPlaneMember(firstVertex.x, firstVertex.y, firstVertex.z)) {
            this.facePlaneCount++;
        }
    }

    private static SortType filterSortType(SortType sortType) {
        // SortBehavior sortBehavior = SodiumClientMod.options().performance.sortBehavior;
        // if (!sortBehavior.sortTypes.contains(sortType)) {
        //     return SortType.NONE;
        // }
        // if (sortBehavior == SortBehavior.ONLY_DYNAMIC_ALL) {
        //     return SortType.DYNAMIC_ALL;
        // }
        return sortType;
    }

    /**
     * Checks if this group builder is relevant for translucency sort triggering. It
     * determines a sort type, which is either no sorting, a static sort or a
     * dynamic sort (section in GFNI only in this case).
     * 
     * See the section on special cases for an explanation of the special sorting
     * cases: https://hackmd.io/@douira100/sodium-sl-gfni#Special-Sorting-Cases
     * 
     * A: If there are no or only one normal, this builder can be considered
     * practically empty.
     * 
     * B: If there are two face planes with opposing normals at the same distance,
     * then
     * they can't be seen through each other and this section can be ignored.
     * 
     * C: If the translucent faces are on the surface of the convex hull of all
     * translucent faces in the section and face outwards, then there is no way to
     * see one through another. Since convex hulls are hard, a simpler case only
     * uses the axis aligned normals: Under the condition that only aligned normals
     * are used in the section, tracking the bounding box of the translucent
     * geometry (the vertices) in the section and then checking if the normal
     * distances line up with the bounding box allows the exclusion of some
     * sections containing a single convex translucent cuboid (of which not all
     * faces need to exist).
     * 
     * D: If there are only two normals which are opposites of
     * each other, then a special fixed sort order is always a correct sort order.
     * This ordering sorts the two sets of face planes by their ascending
     * normal-relative distance. The ordering between the two normals is irrelevant
     * as they can't be seen through each other anyways.
     * 
     * E: If there are only three axis-aligned normals or only two normals if there
     * is at least one unaligned normal, a static topological sort of the
     * see-through graph is enough. The sort is performed on the see-through graph
     * consisting of quads as nodes and edges between two quads if the one can be
     * seen through the other. The see-through condition is not checked transitively
     * which avoids needing to do complex projections of quads onto each other. A
     * static sort exists if this graph is acyclic. In the aforementioned cases, the
     * graph is known to be acyclic. It can also be acyclic if there are more
     * normals, but this would require a search of the graph for cycles.
     * 
     * More heuristics can be performed here to conservatively determine if this
     * section could possibly have more than one translucent sort order.
     * 
     * @return the required sort type to ensure this section always looks correct
     */
    private SortType sortTypeHeuristic() {
        SortBehavior sortBehavior = SodiumClientMod.options().performance.sortBehavior;
        if (sortBehavior == SortBehavior.ONLY_DYNAMIC_ALL) {
            return SortType.DYNAMIC_ALL;
        }

        // special case A
        if (sortBehavior == SortBehavior.ONLY_TRIVIAL || this.facePlaneCount <= 1) {
            return SortType.NONE;
        }

        if (this.unalignedDistances == null) {
            boolean twoOpposingNormals = this.alignedNormalBitmap == OPPOSING_X
                    || this.alignedNormalBitmap == OPPOSING_Y
                    || this.alignedNormalBitmap == OPPOSING_Z;

            // special case B
            // if there are just two normals, they are exact opposites of eachother and they
            // each only have one distance, there is no way to see through one face to the
            // other.
            if (this.facePlaneCount == 2 && twoOpposingNormals) {
                return SortType.NONE;
            }

            // special case C
            // the more complex test that checks for distances aligned with the bounding box
            if (this.facePlaneCount <= ModelQuadFacing.DIRECTIONS) {
                boolean passesBoundingBoxTest = true;
                for (AccumulationGroup accGroup : this.axisAlignedDistances) {
                    if (accGroup == null) {
                        continue;
                    }

                    if (accGroup.relativeDistances.size() > 1) {
                        passesBoundingBoxTest = false;
                        break;
                    }

                    // check the distance against the bounding box
                    float outwardBoundDistance = (accGroup.normal.x() < 0
                            || accGroup.normal.y() < 0
                            || accGroup.normal.z() < 0)
                                    ? accGroup.normal.dot(minBounds)
                                    : accGroup.normal.dot(maxBounds);
                    if (accGroup.relativeDistances.iterator().nextDouble() != outwardBoundDistance) {
                        passesBoundingBoxTest = false;
                        break;
                    }
                }
                if (passesBoundingBoxTest) {
                    return SortType.NONE;
                }
            }

            // special case D
            // there are up to two normals that are opposing, this means no dynamic sorting
            // is necessary. Without static sorting, the geometry to trigger on could be
            // reduced but this isn't done here as we assume static sorting is possible.
            if (twoOpposingNormals || Integer.bitCount(this.alignedNormalBitmap) == 1) {
                return SortType.STATIC_NORMAL_RELATIVE;
            }

            // special case E
            if (Integer.bitCount(this.alignedNormalBitmap) <= 3) {
                return SortType.STATIC_TOPO_ACYCLIC;
            }
        } else if (this.alignedNormalBitmap == 0) {
            if (this.unalignedDistances.size() == 1) {
                // special case D but for one unaligned normal
                return SortType.STATIC_NORMAL_RELATIVE;
            } else if (this.unalignedDistances.size() == 2) {
                // special case D but for two opposing unaligned normals
                var iterator = this.unalignedDistances.values().iterator();
                var normalA = iterator.next().normal;
                var normalB = iterator.next().normal;
                if (normalA.x() == -normalB.x()
                        && normalA.y() == -normalB.y()
                        && normalA.z() == -normalB.z()) {
                    return SortType.STATIC_NORMAL_RELATIVE;
                }
            }
        }

        // special case E
        if (Integer.bitCount(this.alignedNormalBitmap)
                + (this.unalignedDistances == null ? 0 : this.unalignedDistances.size()) <= 2) {
            return SortType.STATIC_TOPO_ACYCLIC;
        }

        // heuristically determine if a topo sort should be attempted, if the attempt
        // fails the sort type is downgraded to DYNAMIC_ALL. If there are no cycles,
        // it's upgraded to acyclic.
        if (this.unalignedQuadCount <= 2 && this.quads.size() <= 400) {
            return SortType.DYNAMIC_TOPO_CYCLIC;
        }

        return SortType.DYNAMIC_ALL;
    }

    public SortType estimateSortType() {
        this.sortType = filterSortType(sortTypeHeuristic());
        return this.sortType;
    }

    private StaticNormalRelativeData constructStaticNormalRelativeData(BuiltSectionMeshParts translucentMesh) {
        int vertexCount = 0;
        VertexRange[] ranges = translucentMesh.getVertexRanges();
        Vector3f[][] centers = new Vector3f[ModelQuadFacing.COUNT][];
        int[] centerCounters = new int[ModelQuadFacing.COUNT];

        for (int i = 0; i < ModelQuadFacing.COUNT; i++) {
            VertexRange range = ranges[i];
            if (range != null) {
                vertexCount += range.vertexCount();
                centers[i] = new Vector3f[range.vertexCount() / TranslucentData.VERTICES_PER_QUAD];
            }
        }

        for (Quad quad : this.quads) {
            var direction = quad.facing.ordinal();
            centers[direction][centerCounters[direction]++] = quad.center;
        }

        var buffer = new NativeBuffer(TranslucentData.vertexCountToIndexBytes(vertexCount));
        IntBuffer bufferBuilder = buffer.getDirectBuffer().asIntBuffer();

        for (int i = 0; i < ModelQuadFacing.COUNT; i++) {
            if (ranges[i] != null) {
                TranslucentData.writeVertexIndexes(bufferBuilder, GFNI.SORTERS[i].sort(centers[i]));
            }
        }

        return new StaticNormalRelativeData(this.sectionPos, buffer, ranges);
    }

    private static VertexRange getUnassignedVertexRange(BuiltSectionMeshParts translucentMesh) {
        VertexRange range = translucentMesh.getVertexRanges()[ModelQuadFacing.UNASSIGNED.ordinal()];

        if (range == null) {
            throw new IllegalStateException("No unassigned data in mesh");
        }

        return range;
    }

    // NOTE: requires filling the contained buffer afterwards
    private StaticTopoAcyclicData constructStaticTopoAcyclicData(BuiltSectionMeshParts translucentMesh) {
        VertexRange range = GroupBuilder.getUnassignedVertexRange(translucentMesh);
        var buffer = new NativeBuffer(TranslucentData.vertexCountToIndexBytes(range.vertexCount()));

        return new StaticTopoAcyclicData(this.sectionPos, buffer, range);
    }

    private DynamicData constructDynamicData(BuiltSectionMeshParts translucentMesh, NativeBuffer reuseBuffer,
            Vector3fc cameraPos) {
        VertexRange range = GroupBuilder.getUnassignedVertexRange(translucentMesh);
        int[] centerCounters = new int[ModelQuadFacing.COUNT];

        for (Quad quad : this.quads) {
            centerCounters[quad.facing.ordinal()]++;
        }

        // do a prefix sum to determine the offsets of where to write the centers
        int quadCount = 0;
        for (int i = 0; i < ModelQuadFacing.COUNT; i++) {
            var newCount = centerCounters[i] + quadCount;
            centerCounters[i] = quadCount;
            quadCount = newCount;
        }

        if (reuseBuffer == null) {
            reuseBuffer = new NativeBuffer(
                    TranslucentData.vertexCountToIndexBytes(quadCount * TranslucentData.VERTICES_PER_QUAD));
        }

        Vector3f[] centers = new Vector3f[quadCount];

        for (int i = 0; i < this.quads.size(); i++) {
            Quad quad = this.quads.get(i);
            centers[centerCounters[quad.facing.ordinal()]++] = quad.center;
        }

        var dynamicData = new DynamicData(this.sectionPos,
                reuseBuffer, range, centers, axisAlignedDistances, unalignedDistances);
        dynamicData.sort(cameraPos);
        return dynamicData;
    }

    public TranslucentData getTranslucentData(BuiltSectionMeshParts translucentMesh, Vector3fc cameraPos) {
        if (this.sortType == SortType.NONE || translucentMesh == null) {
            return new NoneData(this.sectionPos);
        }

        if (this.sortType == SortType.STATIC_NORMAL_RELATIVE) {
            return constructStaticNormalRelativeData(translucentMesh);
        }

        if (SodiumClientMod.options().performance.sortBehavior == SortBehavior.ONLY_DYNAMIC_ALL) {
            return constructDynamicData(translucentMesh, null, cameraPos);
        }

        // from this point on we know the estimated sort type requires direction mixing
        // (no backface culling) and all vertices are in the UNASSIGNED direction.
        NativeBuffer buffer = null;
        if (this.sortType == SortType.STATIC_TOPO_ACYCLIC
                || this.sortType == SortType.DYNAMIC_TOPO_CYCLIC
                || this.sortType == SortType.DYNAMIC_ALL) {
            // TODO: implement topo sort with unaligned quads
            if (this.unalignedQuadCount > 0) {
                unalignedDynamicHits++;
                this.sortType = SortType.DYNAMIC_ALL;
            } else {
                // it can only perform topo sort on acyclic graphs since it has no cycle
                // breaking, but it will detect cycles and bail to DYNAMIC_ALL
                // DYNAMIC_TOPO_CYCLIC if there is a cycle
                var indexData = constructStaticTopoAcyclicData(translucentMesh);
                buffer = indexData.buffer;
                IntBuffer indexBuffer = buffer.getDirectBuffer().asIntBuffer();

                if (topoSortAlignedAcyclic(indexBuffer)) {
                    topoSortHits++;

                    return indexData;
                } else {
                    // TODO: cyclic topo sort with cycle breaking
                    cyclicGraphHits++;

                    this.sortType = SortType.DYNAMIC_ALL;
                }
            }

            System.out.println("topo sort hits: " + topoSortHits + ", cyclic graph hits: " + cyclicGraphHits
                    + ", unaligned dynamic hits: " + unalignedDynamicHits);
        }

        // filter the sort type with the user setting and re-evaluate
        this.sortType = filterSortType(this.sortType);

        if (this.sortType == SortType.NONE) {
            return new NoneData(this.sectionPos);
        }

        if (this.sortType == SortType.DYNAMIC_ALL) {
            return constructDynamicData(translucentMesh, buffer, cameraPos);
        }

        throw new IllegalStateException("Unknown sort type: " + this.sortType);
    }

    private static boolean orthogonalQuadVisible(Quad quad, Quad otherQuad) {
        var otherQuadDirection = otherQuad.facing.ordinal();
        var sign = otherQuad.facing.getSign();

        // this only works because the quads are planar and the extent in the direction
        // of the quad's normal is the same as in the opposite direction
        return sign * quad.extents[otherQuadDirection] > sign * otherQuad.extents[otherQuadDirection];
    }

    /**
     * The index in each node's array in the graph where the number of outgoing
     * edges is stored.
     */
    private static final int OUTGOING_EDGES = ModelQuadFacing.DIRECTIONS;

    private static void makeEdge(int[][] graph, BitArray leafQuads, int fromQuadIndex, int toQuadIndex, int direction) {
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
     * 
     * If it manages to find a topological sort, it populates
     * {@link #topoSortResult} and returns {@link SortType#STATIC_TOPO_ACYCLIC}. If
     * it fails to find a topo sort it returns {@link SortType#DYNAMIC_TOPO_CYCLIC}.
     * 
     * @return if the sort was successful
     */
    private boolean topoSortAlignedAcyclic(IntBuffer indexBuffer) {
        var totalQuadCount = this.quads.size();

        /**
         * The translucent quad visibility graph is stored as an array for each quad.
         * Each quad's array stores the indexes of the quads that can see this quad
         * through them. The edges are stored backwards to avoid using dynamically
         * sized-lists for outgoing edges.
         * 
         * The last entry stores the number of *outgoing* edges.
         */
        int[][] graph = new int[totalQuadCount][ModelQuadFacing.DIRECTIONS + 1];

        // the set of quads that have no outgoing edges
        BitArray leafQuads = new BitArray(totalQuadCount);
        leafQuads.set(0, totalQuadCount);

        for (int i = 0; i < totalQuadCount; i++) {
            for (int j = 0; j < ModelQuadFacing.DIRECTIONS; j++) {
                graph[i][j] = -1;
            }
        }

        // the stash of quads that have not yet been visible to the scanned quads
        BitArray stashedOrthoQuads = new BitArray(totalQuadCount);

        // keep around the allocation of the keys array
        float[] keys = new float[totalQuadCount];

        // to build the graph, perform scans for each direction
        for (int direction = 0; direction < ModelQuadFacing.DIRECTIONS; direction++) {
            ModelQuadFacing facing = ModelQuadFacing.VALUES[direction];
            ModelQuadFacing oppositeFacing = facing.getOpposite();
            int oppositeDirection = oppositeFacing.ordinal();
            int sign = facing.getSign();

            // generate keys for this direction
            for (int i = 0; i < totalQuadCount; i++) {
                // get the extent in the opposite direction of the scan because quads that are
                // visible from a scanning quad should be before it
                Quad quad = this.quads.get(i);
                keys[i] = quad.extents[oppositeDirection] * sign * -1;
            }

            int[] sortedQuads = MergeSort.mergeSort(keys);

            // the index of the last quad facing in scan direction (scanning quad) in the
            // sorted quad array
            int lastScanQuadPos = -1;

            // perform a scan by going through the sorted quads and making edges between the
            // scanning quads and the quads that precede them in the sort order
            stashedOrthoQuads.unset();
            for (int quadIndexPos = 0; quadIndexPos < totalQuadCount; quadIndexPos++) {
                int quadIndex = sortedQuads[quadIndexPos];
                Quad quad = this.quads.get(quadIndex);
                if (quad.facing != facing) {
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
                    Quad stashedOrthoQuad = this.quads.get(stashedQuadIndex);

                    // if it's visible through the current quad, unstash and connect
                    if (orthogonalQuadVisible(quad, stashedOrthoQuad)) {
                        stashedOrthoQuads.unset(stashedQuadIndex);
                        makeEdge(graph, leafQuads, quadIndex, stashedQuadIndex, direction);
                    }
                }

                // check the quads facing in other directions than the scan facing.
                // initially increment to skip the last scan quad.
                for (++lastScanQuadPos; lastScanQuadPos < quadIndexPos; lastScanQuadPos++) {
                    int otherQuadIndex = sortedQuads[lastScanQuadPos];
                    Quad otherQuad = this.quads.get(otherQuadIndex);

                    // discard quads that face in the opposite direction, they are never visible
                    if (otherQuad.facing == oppositeFacing) {
                        continue;
                    }

                    // if it's visible through the current quad, add an edge.
                    // the quad has a direction that is orthogonal to the scan direction, since
                    // opposite quads were just ruled out and same facing quads are handled by the
                    // scan.
                    if (orthogonalQuadVisible(quad, otherQuad)) {
                        makeEdge(graph, leafQuads, quadIndex, otherQuadIndex, direction);
                    } else {
                        // otherwise stash it to check if it's visible by later quads in the scan
                        stashedOrthoQuads.set(otherQuadIndex);
                    }
                }

                // lastScanQuad is now at quadIndexPos
            }
        }

        // the topological sort result of the graph. The order is reversed comapred to
        // how topo sort is usually defined, in that the first index points to the quad
        // that should be rendered first since it has no outgoing edges (and thus no
        // other quads are visible through it).

        // iterate through the set of quads with no outgoing edges until there are none
        // left.
        for (int topoSortPos = 0; topoSortPos < totalQuadCount; topoSortPos++) {
            int nextLeafQuadIndex = leafQuads.nextSetBit(0);

            // if there are no leaf quads but not yet all quads have been processed,
            // there must be a cycle!
            if (nextLeafQuadIndex == -1) {
                // since this method may be called if it's known that there are no cycles, when
                // they are found it's downgraded to DYNAMIC_TOPO_CYCLIC
                return false;
            }

            leafQuads.unset(nextLeafQuadIndex);

            // add it to the topo sort result
            TranslucentData.putQuadVertexIndexes(indexBuffer, nextLeafQuadIndex);

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
