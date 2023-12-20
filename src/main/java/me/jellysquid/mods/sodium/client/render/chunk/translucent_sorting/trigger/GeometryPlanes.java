package me.jellysquid.mods.sodium.client.render.chunk.translucent_sorting.trigger;

import java.util.Collection;

import org.joml.Vector3fc;

import it.unimi.dsi.fastutil.objects.Object2ReferenceOpenHashMap;
import me.jellysquid.mods.sodium.client.model.quad.properties.ModelQuadFacing;
import me.jellysquid.mods.sodium.client.render.chunk.translucent_sorting.TQuad;
import net.minecraft.util.math.ChunkSectionPos;

public class GeometryPlanes {
    private NormalPlanes[] alignedDistances;
    private Object2ReferenceOpenHashMap<Vector3fc, NormalPlanes> unalignedDistances;

    public NormalPlanes[] getAligned() {
        return this.alignedDistances;
    }

    public NormalPlanes[] getAlignedOrCreate() {
        if (this.alignedDistances == null) {
            this.alignedDistances = new NormalPlanes[ModelQuadFacing.DIRECTIONS];
        }
        return this.alignedDistances;
    }

    public Collection<NormalPlanes> getUnaligned() {
        if (this.unalignedDistances == null) {
            return null;
        }
        return this.unalignedDistances.values();
    }

    public Object2ReferenceOpenHashMap<Vector3fc, NormalPlanes> getUnalignedOrCreate() {
        if (this.unalignedDistances == null) {
            this.unalignedDistances = new Object2ReferenceOpenHashMap<>();
        }
        return this.unalignedDistances;
    }

    public Collection<Vector3fc> getUnalignedNormals() {
        if (this.unalignedDistances == null) {
            return null;
        }
        return this.unalignedDistances.keySet();
    }

    NormalPlanes getPlanesForNormal(NormalList normalList) {
        var normal = normalList.getNormal();
        if (normal.isAligned()) {
            if (this.alignedDistances == null) {
                return null;
            }
            return this.alignedDistances[normal.getAlignedDirection()];
        } else {
            if (this.unalignedDistances == null) {
                return null;
            }
            return this.unalignedDistances.get(normal);
        }
    }

    public void addAlignedPlane(ChunkSectionPos sectionPos, int direction, float distance) {
        var alignedDistances = this.getAlignedOrCreate();
        var normalPlanes = alignedDistances[direction];
        if (normalPlanes == null) {
            normalPlanes = new NormalPlanes(sectionPos, direction);
            alignedDistances[direction] = normalPlanes;
        }
        normalPlanes.addPlaneMember(distance);
    }

    public void addDoubleSidedPlane(ChunkSectionPos sectionPos, int axis, float distance) {
        this.addAlignedPlane(sectionPos, axis, distance);
        this.addAlignedPlane(sectionPos, axis + 3, -distance);
    }

    public void addUnalignedPlane(ChunkSectionPos sectionPos, Vector3fc normal, float distance) {
        var unalignedDistances = this.getUnalignedOrCreate();
        var normalPlanes = unalignedDistances.get(normal);
        if (normalPlanes == null) {
            normalPlanes = new NormalPlanes(sectionPos, normal);
            unalignedDistances.put(normal, normalPlanes);
        }
        normalPlanes.addPlaneMember(distance);
    }

    public void addQuadPlane(ChunkSectionPos sectionPos, TQuad quad) {
        var facing = quad.getFacing();
        if (facing.isAligned()) {
            this.addAlignedPlane(sectionPos, facing.ordinal(), quad.getDotProduct());
        } else {
            this.addUnalignedPlane(sectionPos, quad.getQuantizedNormal(), quad.getDotProduct());
        }
    }

    private void prepareAndInsert(Object2ReferenceOpenHashMap<Vector3fc, float[]> distancesByNormal) {
        if (this.alignedDistances != null) {
            for (var normalPlanes : this.alignedDistances) {
                if (normalPlanes != null) {
                    normalPlanes.prepareAndInsert(distancesByNormal);
                }
            }
        }
        if (this.unalignedDistances != null) {
            for (var normalPlanes : this.unalignedDistances.values()) {
                normalPlanes.prepareAndInsert(distancesByNormal);
            }
        }
    }

    public void prepareIntegration() {
        this.prepareAndInsert(null);
    }

    public Object2ReferenceOpenHashMap<Vector3fc, float[]> prepareAndGetDistances() {
        var distancesByNormal = new Object2ReferenceOpenHashMap<Vector3fc, float[]>(10);
        this.prepareAndInsert(distancesByNormal);
        return distancesByNormal;
    }

    public static GeometryPlanes fromQuadLists(ChunkSectionPos sectionPos, TQuad[] quads) {
        var geometryPlanes = new GeometryPlanes();
        for (var quad : quads) {
            geometryPlanes.addQuadPlane(sectionPos, quad);
        }
        return geometryPlanes;
    }
}
