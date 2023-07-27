package me.jellysquid.mods.sodium.client.render.chunk.gfni;

import org.joml.Vector3f;
import org.joml.Vector3fc;

import it.unimi.dsi.fastutil.ints.Int2ReferenceLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.ReferenceArrayList;
import me.jellysquid.mods.sodium.client.model.quad.properties.ModelQuadFacing;
import net.minecraft.util.math.ChunkSectionPos;

public class GroupBuilder {
    public static final Vector3fc[] ALIGNED_NORMALS = new Vector3fc[ModelQuadFacing.DIRECTIONS];

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

    @SuppressWarnings("unchecked")
    public final ReferenceArrayList<Vector3f>[] quadCenters = new ReferenceArrayList[ModelQuadFacing.DIRECTIONS];

    public GroupBuilder(ChunkSectionPos sectionPos) {
        this.sectionPos = sectionPos;
    }

    public void appendQuadCenter(ModelQuadFacing facing, float xSum, float ySum, float zSum) {
        var list = this.quadCenters[facing.ordinal()];
        if (list == null) {
            list = new ReferenceArrayList<>();
            this.quadCenters[facing.ordinal()] = list;
        }
        list.add(new Vector3f(xSum * 0.25f, ySum * 0.25f, zSum * 0.25f));
    }

    public void addAlignedFace(ModelQuadFacing facing, float vertexX, float vertexY, float vertexZ) {
        if (facing == ModelQuadFacing.UNASSIGNED) {
            throw new IllegalArgumentException("Cannot add an unaligned face with addAlignedFace()");
        }

        if (this.axisAlignedDistances == null) {
            this.axisAlignedDistances = new AccumulationGroup[ModelQuadFacing.DIRECTIONS];
        }

        int index = facing.ordinal();
        AccumulationGroup distances = this.axisAlignedDistances[index];

        if (distances == null) {
            distances = new AccumulationGroup(sectionPos, ALIGNED_NORMALS[index], index);
            this.axisAlignedDistances[index] = distances;
            this.alignedNormalBitmap |= 1 << index;
        }

        addVertex(distances, vertexX, vertexY, vertexZ);
    }

    public void updateAlignedBounds(float vertexX, float vertexY, float vertexZ) {
        if (this.unalignedDistances == null) {
            minBounds.x = Math.min(minBounds.x, vertexX);
            minBounds.y = Math.min(minBounds.y, vertexY);
            minBounds.z = Math.min(minBounds.z, vertexZ);

            maxBounds.x = Math.max(maxBounds.x, vertexX);
            maxBounds.y = Math.max(maxBounds.y, vertexY);
            maxBounds.z = Math.max(maxBounds.z, vertexZ);
        }
    }

    public void addUnalignedFace(int normalX, int normalY, int normalZ,
            float vertexX, float vertexY, float vertexZ) {
        if (this.unalignedDistances == null) {
            this.unalignedDistances = new Int2ReferenceLinkedOpenHashMap<>(4);
        }

        // the key for the hash map is the normal packed into an int
        // the lowest byte is 0xFF to prevent collisions with axis-aligned normals
        // (assuming quantization with 32, which is 5 bits per component)
        int normalKey = 0xFF | (normalX & 0xFF << 8) | (normalY & 0xFF << 15) | (normalZ & 0xFF << 22);
        AccumulationGroup distances = this.unalignedDistances.get(normalKey);

        if (distances == null) {
            // actually normalize the vector to ensure it's a unit vector
            // for the rest of the process which requires that
            Vector3f normal = new Vector3f(normalX, normalY, normalZ);
            normal.normalize();
            distances = new AccumulationGroup(sectionPos, normal, normalKey);
            this.unalignedDistances.put(normalKey, distances);
        }

        addVertex(distances, vertexX, vertexY, vertexZ);
    }

    private void addVertex(AccumulationGroup accGroup, float vertexX, float vertexY, float vertexZ) {
        if (accGroup.add(vertexX, vertexY, vertexZ)) {
            this.facePlaneCount++;
        }
    }

    AccumulationGroup getGroupForNormal(NormalList normalList) {
        int groupBuilderKey = normalList.getGroupBuilderKey();
        if (groupBuilderKey < 0xFF) {
            if (this.axisAlignedDistances == null) {
                return null;
            }
            return this.axisAlignedDistances[groupBuilderKey];
        } else {
            if (this.unalignedDistances == null) {
                return null;
            }
            return this.unalignedDistances.get(groupBuilderKey);
        }
    }

    /**
     * Checks if this group builder is relevant for translucency sort triggering.
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
     * see one through the other. Since convex nulls are hard, a simpler case only
     * uses the axis aligned normals: Under the condition that only aligned normals
     * are used in the section, tracking the bounding box of the translucent
     * geometry (the vertices) in the section and then checking if the normal
     * distances line up with the bounding box allows the exclusion of some
     * sections containing a single convex translucent cuboid (of which not all
     * faces need to exist).
     * 
     * D1: If there are up to two normals with each up to two distances, then only
     * the larger distance of each normal needs to be triggered on.
     * 
     * TODO: However, for this to work, the initial sort needs to happen fully above
     * or below both of the distances. Otherwise there can be cases where it's
     * wrong.
     * 
     * D2: More generally, if there are only two normals which are opposites of
     * each other, then a special fixed sort order is always a correct sort order.
     * This ordering sorts the two sets of face planes by their ascending
     * normal-relative distance. The ordering between the two normals is irrelevant
     * as they can't be seen through each other anyways. This case in includes case
     * D1 and makes it unnecessary. Sections in D2 don't need to be triggered on at
     * all. (not yet implemented since there is no sorting backend yet)
     * 
     * More heuristics can be performed here to conservatively determine if this
     * section could possibly have more than one translucent sort order.
     * 
     * @return true if this group builder is relevant
     */
    SortType getSortTypeAndSimplify() {
        // special case A
        if (this.facePlaneCount <= 1) {
            return SortType.NONE;
        }

        if (this.unalignedDistances == null) {
            boolean twoOpposingNormals = this.alignedNormalBitmap == 0b11
                    || this.alignedNormalBitmap == 0b1100
                    || this.alignedNormalBitmap == 0b110000;

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

            // special case D1
            if (this.facePlaneCount <= 2 || this.facePlaneCount <= 4 && twoOpposingNormals) {
                // remove the lesser distance of each normal, even if it only has one
                for (AccumulationGroup accGroup : this.axisAlignedDistances) {
                    if (accGroup == null) {
                        continue;
                    }

                    var distanceIterator = accGroup.relativeDistances.iterator();
                    var lesserDistance = distanceIterator.nextDouble();
                    if (distanceIterator.hasNext()) {
                        lesserDistance = Math.min(lesserDistance, distanceIterator.nextDouble());
                    }
                    accGroup.relativeDistances.remove(lesserDistance);
                    this.facePlaneCount--;

                    if (accGroup.relativeDistances.isEmpty()) {
                        this.axisAlignedDistances[accGroup.groupBuilderKey] = null;
                        this.alignedNormalBitmap ^= 1 << accGroup.groupBuilderKey;
                    }
                }

                // in case D1 the group will always still need sorting, just now less often.
                // the case where the only remaining face plane is removed is handled in A.
                return SortType.STATIC_NORMAL_RELATIVE;
            }
        }

        return SortType.DYNAMIC;
    }
}
