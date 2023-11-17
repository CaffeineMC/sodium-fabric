package me.jellysquid.mods.sodium.client.render.chunk.translucent_sorting;

import org.joml.Vector3f;
import org.joml.Vector3fc;

import it.unimi.dsi.fastutil.ints.Int2ReferenceLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.ReferenceArrayList;
import me.jellysquid.mods.sodium.client.SodiumClientMod;
import me.jellysquid.mods.sodium.client.gui.SodiumGameOptions.SortBehavior;
import me.jellysquid.mods.sodium.client.model.quad.ModelQuadView;
import me.jellysquid.mods.sodium.client.model.quad.properties.ModelQuadFacing;
import me.jellysquid.mods.sodium.client.render.chunk.compile.pipeline.FluidRenderer;
import me.jellysquid.mods.sodium.client.render.chunk.data.BuiltSectionMeshParts;
import me.jellysquid.mods.sodium.client.render.chunk.vertex.format.ChunkVertexEncoder;
import me.jellysquid.mods.sodium.client.util.NativeBuffer;
import net.minecraft.util.math.ChunkSectionPos;
import java.util.Arrays;

/**
 * The translucent geometry collector collects the data from the renderers and
 * builds data structures for either dynamic triggering or static sorting. It
 * determines the best sort type for the section and constructs various types of
 * translucent data objects that then perform sorting and get registered with
 * GFNI for triggering.
 *
 * TODO:
 * - use continuous arrays for the quad centers and quad storage if necessary
 * - use more accurate normals for unaligned topo sort?
 * - optionally add a way to attempt full acyclic topo sort even if the
 * heuristic doesn't expect it to be possible. The caveat is that this costs
 * doing it once without invisible quad exclusion and once with if the first
 * attempt fails.
 */
public class TranslucentGeometryCollector {
    AccumulationGroup[] axisAlignedDistances;
    Int2ReferenceLinkedOpenHashMap<AccumulationGroup> unalignedDistances;

    private final ChunkSectionPos sectionPos;
    private int facePlaneCount = 0;
    int alignedNormalBitmap = 0;
    private Vector3f minBounds = new Vector3f(16, 16, 16);
    private Vector3f maxBounds = new Vector3f(0, 0, 0);

    @SuppressWarnings("unchecked")
    private ReferenceArrayList<TQuad>[] quadLists = new ReferenceArrayList[ModelQuadFacing.COUNT];
    private TQuad[] quads;

    private SortType sortType;

    private boolean quadHashPresent = false;
    private int quadHash = 0;

    public TranslucentGeometryCollector(ChunkSectionPos sectionPos) {
        this.sectionPos = sectionPos;
    }

    private static final float INV_QUANTIZE_EPSILON = 256f;
    private static final float QUANTIZE_EPSILON = 1f / INV_QUANTIZE_EPSILON;

    static {
        // ensure it fits with the fluid renderer epsilon and that it's a power-of-two
        // fraction
        var targetEpsilon = FluidRenderer.EPSILON * 2.1f;
        if (QUANTIZE_EPSILON <= targetEpsilon && Integer.bitCount((int) INV_QUANTIZE_EPSILON) == 1) {
            throw new RuntimeException("epsilon is invalid: " + QUANTIZE_EPSILON);
        }
    }

    private static float quantizeEpsilon(float value) {
        return (float) Math.floor(value * INV_QUANTIZE_EPSILON + 0.5) * QUANTIZE_EPSILON;
    }

    public void appendQuad(ModelQuadView quadView, ChunkVertexEncoder.Vertex[] vertices, ModelQuadFacing facing) {
        float xSum = 0;
        float ySum = 0;
        float zSum = 0;

        // keep track of distinct vertices to compute the center accurately for
        // degenerate quads
        float lastX = vertices[3].x;
        float lastY = vertices[3].y;
        float lastZ = vertices[3].z;
        int uniqueQuads = 0;

        float negXExtent = Float.POSITIVE_INFINITY;
        float negYExtent = Float.POSITIVE_INFINITY;
        float negZExtent = Float.POSITIVE_INFINITY;
        float posXExtent = Float.NEGATIVE_INFINITY;
        float posYExtent = Float.NEGATIVE_INFINITY;
        float posZExtent = Float.NEGATIVE_INFINITY;

        for (int i = 0; i < 4; i++) {
            float x = vertices[i].x;
            float y = vertices[i].y;
            float z = vertices[i].z;

            // TODO: see if this is faster than using Math.min and Math.max
            if (x < negXExtent) {
                negXExtent = x;
            } else if (x > posXExtent) {
                posXExtent = x;
            }
            if (y < negYExtent) {
                negYExtent = y;
            } else if (y > posYExtent) {
                posYExtent = y;
            }
            if (z < negZExtent) {
                negZExtent = z;
            } else if (z > posZExtent) {
                posZExtent = z;
            }

            if (x != lastX || y != lastY || z != lastZ) {
                xSum += x;
                ySum += y;
                zSum += z;
                uniqueQuads++;
            }
            if (i != 3) {
                lastX = x;
                lastY = y;
                lastZ = z;
            }
        }

        var centerX = quantizeEpsilon(xSum / uniqueQuads);
        var centerY = quantizeEpsilon(ySum / uniqueQuads);
        var centerZ = quantizeEpsilon(zSum / uniqueQuads);
        var center = new Vector3f(centerX, centerY, centerZ);

        negXExtent = quantizeEpsilon(negXExtent);
        negYExtent = quantizeEpsilon(negYExtent);
        negZExtent = quantizeEpsilon(negZExtent);
        posXExtent = quantizeEpsilon(posXExtent);
        posYExtent = quantizeEpsilon(posYExtent);
        posZExtent = quantizeEpsilon(posZExtent);

        if (facing != ModelQuadFacing.UNASSIGNED && this.unalignedDistances == null) {
            minBounds.x = Math.min(minBounds.x, negXExtent);
            minBounds.y = Math.min(minBounds.y, negYExtent);
            minBounds.z = Math.min(minBounds.z, negZExtent);
            maxBounds.x = Math.max(maxBounds.x, posXExtent);
            maxBounds.y = Math.max(maxBounds.y, posYExtent);
            maxBounds.z = Math.max(maxBounds.z, posZExtent);
        }

        // POS_X, POS_Y, POS_Z, NEG_X, NEG_Y, NEG_Z
        float[] extents = new float[] { posXExtent, posYExtent, posZExtent, negXExtent, negYExtent, negZExtent };

        // TODO: does it make a difference if we compute the center as the average of
        // the unique vertices or as the center of the extents? (the latter would be
        // less work)

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
        } else {
            if (this.axisAlignedDistances == null) {
                this.axisAlignedDistances = new AccumulationGroup[ModelQuadFacing.DIRECTIONS];
            }

            int quadDirection = facing.ordinal();
            accGroup = this.axisAlignedDistances[quadDirection];

            if (accGroup == null) {
                accGroup = new AccumulationGroup(sectionPos, ModelQuadFacing.NORMALS[quadDirection], quadDirection);
                this.axisAlignedDistances[quadDirection] = accGroup;
                this.alignedNormalBitmap |= 1 << quadDirection;
            }

            quadList.add(new TQuad(facing, accGroup.normal, center, extents));
        }

        // use the center here because it's quantized while the vertices themselves
        // aren't. If they were to be mixed this would cause issues in the sorting.
        if (accGroup.addPlaneMember(centerX, centerY, centerZ)) {
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
        switch (sortBehavior) {
            case OFF:
                return SortType.NONE;
            case STATIC:
                if (sortType == SortType.NONE) {
                    return sortType;
                } else {
                    return SortType.STATIC_NORMAL_RELATIVE;
                }
            case DYNAMIC:
                return sortType;
            default:
                throw new IllegalStateException("Unknown sort behavior: " + sortBehavior);
        }
    }

    /**
     * Array of how many quads a section can have with a given number of unique
     * normals so that a static topo sort is attempted on it. -1 means the value is
     * unused and doesn't make sense to give.
     */
    private static int[] STATIC_TOPO_SORT_ATTEMPT_LIMITS = new int[] { -1, -1, 250, 100, 50, 30 };

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
            boolean twoOpposingNormals = this.alignedNormalBitmap == ModelQuadFacing.OPPOSING_X
                    || this.alignedNormalBitmap == ModelQuadFacing.OPPOSING_Y
                    || this.alignedNormalBitmap == ModelQuadFacing.OPPOSING_Z;

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

        // use the given set of quad count limits to determine if a static topo sort
        // should be attempted
        var uniqueNormals = Integer.bitCount(this.alignedNormalBitmap)
                + (this.unalignedDistances == null ? 0 : this.unalignedDistances.size());
        uniqueNormals = Math.max(Math.min(uniqueNormals, STATIC_TOPO_SORT_ATTEMPT_LIMITS.length - 1), 2);
        if (this.quads.length <= STATIC_TOPO_SORT_ATTEMPT_LIMITS[uniqueNormals]) {
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

    private TranslucentData makeNewTranslucentData(BuiltSectionMeshParts translucentMesh, Vector3fc cameraPos) {
        if (this.sortType == SortType.NONE) {
            return AnyOrderData.fromMesh(translucentMesh, quads, sectionPos, null);
        }

        if (this.sortType == SortType.STATIC_NORMAL_RELATIVE) {
            return StaticNormalRelativeData.fromMesh(translucentMesh, this.quads, sectionPos, this);
        }

        // from this point on we know the estimated sort type requires direction mixing
        // (no backface culling) and all vertices are in the UNASSIGNED direction.
        NativeBuffer buffer = PresentTranslucentData.nativeBufferForQuads(this.quads);
        if (this.sortType == SortType.STATIC_TOPO_ACYCLIC) {
            var result = StaticTopoAcyclicData.fromMesh(translucentMesh, this.quads, sectionPos, buffer);
            if (result != null) {
                return result;
            }
            this.sortType = SortType.DYNAMIC_ALL;
        }

        // filter the sort type with the user setting and re-evaluate
        this.sortType = filterSortType(this.sortType);

        if (this.sortType == SortType.NONE) {
            return AnyOrderData.fromMesh(translucentMesh, quads, sectionPos, buffer);
        }

        if (this.sortType == SortType.DYNAMIC_ALL) {
            return DynamicData.fromMesh(translucentMesh, cameraPos, quads, sectionPos, this, buffer);
        }

        throw new IllegalStateException("Unknown sort type: " + this.sortType);
    }

    private int getQuadHash(TQuad[] quads) {
        if (this.quadHashPresent) {
            return this.quadHash;
        }

        for (int i = 0; i < quads.length; i++) {
            var quad = quads[i];
            this.quadHash = this.quadHash * 31 + quad.getQuadHash() + i * 3;
        }
        return this.quadHash;
    }

    public TranslucentData getTranslucentData(
            TranslucentData oldData, BuiltSectionMeshParts translucentMesh, Vector3fc cameraPos) {
        // means there is no translucent geometry
        if (translucentMesh == null) {
            return new NoData(sectionPos);
        }

        // re-use the original translucent data if it's the same. This reduces the
        // amount of generated and uploaded index data when sections are rebuilt without
        // relevant changes to translucent geometry. Rebuilds happen when any part of
        // the section changes, including the here irrelevant cases of changes to opaque
        // geometry or light levels.
        if (oldData != null) {
            // for the NONE sort type the ranges need to be the same, the actual geometry
            // doesn't matter
            if (this.sortType == SortType.NONE && oldData instanceof AnyOrderData oldAnyData
                    && oldAnyData.getLength() == this.quads.length
                    && Arrays.equals(oldAnyData.getVertexRanges(), translucentMesh.getVertexRanges())) {
                oldAnyData.setReuseUploadedData();
                return oldAnyData;
            }

            // for the other sort types the geometry needs to be the same (checked with
            // length and hash)
            if (oldData instanceof PresentTranslucentData oldPresentData) {
                if (oldPresentData.getLength() == this.quads.length
                        && oldPresentData.getQuadHash() == getQuadHash(this.quads)) {
                    oldPresentData.setReuseUploadedData();
                    return oldPresentData;
                }
            }
        }

        var newData = makeNewTranslucentData(translucentMesh, cameraPos);
        if (newData instanceof PresentTranslucentData presentData) {
            presentData.setQuadHash(getQuadHash(this.quads));
        }
        return newData;
    }

    AccumulationGroup getGroupForNormal(NormalList normalList) {
        int collectorKey = normalList.getCollectorKey();
        if (collectorKey < 0xFF) {
            if (this.axisAlignedDistances == null) {
                return null;
            }
            return this.axisAlignedDistances[collectorKey];
        } else {
            if (this.unalignedDistances == null) {
                return null;
            }
            return this.unalignedDistances.get(collectorKey);
        }
    }
}
