package me.jellysquid.mods.sodium.client.render.chunk.gfni;

import org.joml.Vector3f;
import org.joml.Vector3fc;

import it.unimi.dsi.fastutil.ints.Int2ReferenceLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.ReferenceArrayList;
import me.jellysquid.mods.sodium.client.SodiumClientMod;
import me.jellysquid.mods.sodium.client.gui.SodiumGameOptions.SortBehavior;
import me.jellysquid.mods.sodium.client.model.quad.ModelQuadView;
import me.jellysquid.mods.sodium.client.model.quad.properties.ModelQuadFacing;
import me.jellysquid.mods.sodium.client.render.chunk.data.BuiltSectionMeshParts;
import me.jellysquid.mods.sodium.client.render.chunk.vertex.format.ChunkVertexEncoder;
import net.minecraft.util.math.ChunkSectionPos;

/**
 * The translucent geometry collector collects the data from the renderers and
 * builds data structures for either dynamic triggering or static sorting. It
 * determines the best sort type for the section and constructs various types of
 * translucent data objects that then perform sorting and get registered with
 * GFNI for triggering.
 * 
 * TODO: can use a bunch more optimizations, this is a prototype.
 * TODO list:
 * - use continuous arrays for the quad centers and quad storage
 * - use more accurate normals for unaligned topo sort? do we do topo sort on
 * unaligned faces at all?
 * - bail early during rendering if we decide not to topo sort, then we don't
 * need to gather all of the data
 * - detail how the graph construction and the topo sort works in the GFNI doc
 * - optionally add a way to attempt full acyclic topo sort even if the
 * heuristic doesn't expect it to be possible. The caveat is that this costs
 * doing it once without invisible quad exclusion and once with if the first
 * attempt fails.
 * - disable translucent data collection when setting is set to OFF to prevent
 * overhead if no sorting is wanted.
 */
public class TranslucentGeometryCollector {
    private static final Vector3fc[] ALIGNED_NORMALS = new Vector3fc[ModelQuadFacing.DIRECTIONS];

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

    private final ChunkSectionPos sectionPos;
    private int facePlaneCount = 0;
    int alignedNormalBitmap = 0;
    private Vector3f minBounds = new Vector3f(16, 16, 16);
    private Vector3f maxBounds = new Vector3f(0, 0, 0);

    private int unalignedQuadCount = 0;

    @SuppressWarnings("unchecked")
    private ReferenceArrayList<TQuad>[] quadLists = new ReferenceArrayList[ModelQuadFacing.COUNT];
    private TQuad[] quads;

    private SortType sortType;

    public TranslucentGeometryCollector(ChunkSectionPos sectionPos) {
        this.sectionPos = sectionPos;
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
        var quadList = this.quadLists[facing.ordinal()];
        if (quadList == null) {
            quadList = new ReferenceArrayList<>();
            this.quadLists[facing.ordinal()] = quadList;
        }

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

            quadList.add(new TQuad(facing, accGroup.normal, center, extents));
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

            quadList.add(new TQuad(facing, accGroup.normal, center, extents));
        }

        var firstVertex = vertices[0];
        if (accGroup.addPlaneMember(firstVertex.x, firstVertex.y, firstVertex.z)) {
            this.facePlaneCount++;
        }
    }

    /**
     * Filters the given sort type to fit within the selected sorting mode. If it
     * doesn't match, then it's set to the NONE sort type.
     * 
     * @param sortType the sort type to filter
     */
    private static SortType filterSortType(SortType sortType) {
        SortBehavior sortBehavior = SodiumClientMod.options().performance.sortBehavior;
        if (!sortBehavior.sortTypes.contains(sortType)) {
            return SortType.NONE;
        }
        return sortType;
    }

    /**
     * Determines the sort type for the collected geometry from the section. It
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

        // special case A
        if (sortBehavior == SortBehavior.OFF || this.facePlaneCount <= 1) {
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

        return SortType.DYNAMIC_ALL;
    }

    public SortType finishRendering() {
        // combine the quads into one array
        int totalQuadCount = 0;
        for (var quadList : this.quadLists) {
            if (quadList != null) {
                totalQuadCount += quadList.size();
            }
        }
        this.quads = new TQuad[totalQuadCount];
        int quadIndex = 0;
        for (var quadList : this.quadLists) {
            if (quadList != null) {
                for (var quad : quadList) {
                    this.quads[quadIndex++] = quad;
                }
            }
        }
        this.quadLists = null;

        this.sortType = filterSortType(sortTypeHeuristic());
        return this.sortType;
    }

    public TranslucentData getTranslucentData(BuiltSectionMeshParts translucentMesh, Vector3fc cameraPos) {
        // means there is no translucent geometry
        if (translucentMesh == null) {
            return new NoData(sectionPos);
        }

        if (this.sortType == SortType.NONE) {
            return AnyOrderData.fromMesh(translucentMesh, quads, sectionPos);
        }

        if (this.sortType == SortType.STATIC_NORMAL_RELATIVE) {
            return StaticNormalRelativeData.fromMesh(translucentMesh, this.quads, sectionPos, this);
        }

        // from this point on we know the estimated sort type requires direction mixing
        // (no backface culling) and all vertices are in the UNASSIGNED direction.
        if (this.sortType == SortType.STATIC_TOPO_ACYCLIC) {
            if (this.unalignedQuadCount > 0) {
                // TODO: implement topo sort with unaligned quads
                this.sortType = SortType.DYNAMIC_ALL;
            } else {
                return StaticTopoAcyclicData.fromMesh(translucentMesh, this.quads, sectionPos);
            }
        }

        // filter the sort type with the user setting and re-evaluate
        this.sortType = filterSortType(this.sortType);

        if (this.sortType == SortType.NONE) {
            return AnyOrderData.fromMesh(translucentMesh, quads, sectionPos);
        }

        if (this.sortType == SortType.DYNAMIC_ALL) {
            return DynamicData.fromMesh(translucentMesh, cameraPos, quads, sectionPos, this);
        }

        throw new IllegalStateException("Unknown sort type: " + this.sortType);
    }
}
