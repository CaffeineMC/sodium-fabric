package me.jellysquid.mods.sodium.client.render.chunk.gfni;

import org.joml.Vector3f;
import org.joml.Vector3fc;

import it.unimi.dsi.fastutil.objects.Object2ReferenceOpenHashMap;
import me.jellysquid.mods.sodium.client.model.quad.properties.ModelQuadFacing;
import net.minecraft.util.math.ChunkSectionPos;

public class GroupBuilder {
    public static final Vector3fc[] ALIGNED_NORMALS = new Vector3fc[ModelQuadFacing.DIRECTIONS.length];

    static {
        for (int i = 0; i < ModelQuadFacing.DIRECTIONS.length; i++) {
            ALIGNED_NORMALS[i] = new Vector3f(ModelQuadFacing.DIRECTIONS[i].toDirection().getUnitVector());
        }
    }

    AccumulationGroup[] alignedFaceDistances = new AccumulationGroup[ModelQuadFacing.DIRECTIONS.length];
    Object2ReferenceOpenHashMap<Vector3fc, AccumulationGroup> unalignedDistances = new Object2ReferenceOpenHashMap<>(
            4);
    ChunkSectionPos sectionPos;

    public GroupBuilder(ChunkSectionPos sectionPos) {
        this.sectionPos = sectionPos;
    }

    public void addAlignedFace(ModelQuadFacing facing, float vertexX, float vertexY, float vertexZ) {
        if (facing == ModelQuadFacing.UNASSIGNED) {
            throw new IllegalArgumentException("Cannot add an unaligned face with addAlignedFace()");
        }

        int index = facing.ordinal();
        AccumulationGroup distances = this.alignedFaceDistances[index];

        if (distances == null) {
            distances = new AccumulationGroup(sectionPos, ALIGNED_NORMALS[index]);
            this.alignedFaceDistances[index] = distances;
        }

        distances.add(vertexX, vertexY, vertexZ);
    }

    public void addUnalignedFace(float normalX, float normalY, float normalZ,
            float vertexX, float vertexY, float vertexZ) {
        Vector3fc normal = new Vector3f(normalX, normalY, normalZ);
        AccumulationGroup distances = this.unalignedDistances.get(normal);

        if (distances == null) {
            distances = new AccumulationGroup(sectionPos, normal);
            this.unalignedDistances.put(normal, distances);
        }

        distances.add(vertexX, vertexY, vertexZ);
    }
}
