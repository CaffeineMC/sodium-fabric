package me.jellysquid.mods.sodium.client.render.chunk.gfni;

import org.joml.Vector3f;
import org.joml.Vector3fc;

import it.unimi.dsi.fastutil.ints.Int2ReferenceLinkedOpenHashMap;
import me.jellysquid.mods.sodium.client.model.quad.properties.ModelQuadFacing;
import net.minecraft.util.math.ChunkSectionPos;

public class GroupBuilder {
    public static final Vector3fc[] ALIGNED_NORMALS = new Vector3fc[ModelQuadFacing.DIRECTIONS.length];

    static {
        for (int i = 0; i < ModelQuadFacing.DIRECTIONS.length; i++) {
            ALIGNED_NORMALS[i] = new Vector3f(ModelQuadFacing.DIRECTIONS[i].toDirection().getUnitVector());
        }
    }

    AccumulationGroup[] axisAlignedDistances;
    Int2ReferenceLinkedOpenHashMap<AccumulationGroup> unalignedDistances;

    final ChunkSectionPos sectionPos;
    private int facePlaneCount = 0;

    public GroupBuilder(ChunkSectionPos sectionPos) {
        this.sectionPos = sectionPos;
    }

    public void addAlignedFace(ModelQuadFacing facing, float vertexX, float vertexY, float vertexZ) {
        if (facing == ModelQuadFacing.UNASSIGNED) {
            throw new IllegalArgumentException("Cannot add an unaligned face with addAlignedFace()");
        }

        if (this.axisAlignedDistances == null) {
            this.axisAlignedDistances = new AccumulationGroup[ModelQuadFacing.DIRECTIONS.length];
        }

        int index = facing.ordinal();
        AccumulationGroup distances = this.axisAlignedDistances[index];

        if (distances == null) {
            distances = new AccumulationGroup(sectionPos, ALIGNED_NORMALS[index], index);
            this.axisAlignedDistances[index] = distances;
        }

        addVertex(distances, vertexX, vertexY, vertexZ);
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
     * Checks if this group builder is relevant for translucency sort triggering. If
     * there are no or only one normal, this builder can be considered practically
     * empty.
     * 
     * More heuristics can be performed here to conservatively determine if this
     * section could possibly have more than one translucent sort order.
     * 
     * @return true if this group builder is relevant
     */
    boolean isRelevant() {
        return this.facePlaneCount > 1;
    }
}
