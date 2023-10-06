package me.jellysquid.mods.sodium.client.render.chunk.gfni;

import java.nio.IntBuffer;

import org.joml.Vector3fc;

import it.unimi.dsi.fastutil.objects.Object2ReferenceOpenHashMap;
import me.jellysquid.mods.sodium.client.gl.util.VertexRange;
import me.jellysquid.mods.sodium.client.model.quad.properties.ModelQuadFacing;
import me.jellysquid.mods.sodium.client.render.chunk.data.BuiltSectionMeshParts;
import me.jellysquid.mods.sodium.client.util.NativeBuffer;
import net.minecraft.util.math.ChunkSectionPos;

public class DynamicData extends MixedDirectionData {
    private final TQuad[] quads;
    private TranslucentGeometryCollector collector;
    private Object2ReferenceOpenHashMap<Vector3fc, double[]> distancesByNormal;

    DynamicData(ChunkSectionPos sectionPos,
            NativeBuffer buffer, VertexRange range, TQuad[] quads,
            TranslucentGeometryCollector collector,
            Object2ReferenceOpenHashMap<Vector3fc, double[]> distancesByNormal) {
        super(sectionPos, buffer, range);
        this.quads = quads;
        this.collector = collector;
        this.distancesByNormal = distancesByNormal;
    }

    @Override
    public SortType getSortType() {
        return SortType.DYNAMIC_ALL;
    }

    @Override
    public void sort(Vector3fc cameraPos) {
        IntBuffer indexBuffer = this.buffer.getDirectBuffer().asIntBuffer();

        if (!ComplexSorting.topoSortDepthFirstCyclic(indexBuffer, this.quads, this.distancesByNormal, cameraPos)) {
            // if a camera position is given, no cycle should be possible
            throw new IllegalStateException("Failed to sort");
        }
    }

    TranslucentGeometryCollector getCollector() {
        return this.collector;
    }

    void finishIntegration() {
        this.collector = null;
    }

    static DynamicData fromMesh(BuiltSectionMeshParts translucentMesh,
            Vector3fc cameraPos, TQuad[] quads, ChunkSectionPos sectionPos, TranslucentGeometryCollector collector) {
        // prepare accumulation groups for GFNI integration and copy
        var size = 0;
        if (collector.axisAlignedDistances != null) {
            size += Integer.bitCount(collector.alignedNormalBitmap);
        }
        if (collector.unalignedDistances != null) {
            size += collector.unalignedDistances.size();
        }
        var distancesByNormal = new Object2ReferenceOpenHashMap<Vector3fc, double[]>(size);
        if (collector.axisAlignedDistances != null) {
            for (int direction = 0; direction < ModelQuadFacing.DIRECTIONS; direction++) {
                var accGroup = collector.axisAlignedDistances[direction];
                if (accGroup != null) {
                    accGroup.prepareIntegration();
                    distancesByNormal.put(accGroup.normal, accGroup.facePlaneDistances);
                }
            }
        }
        if (collector.unalignedDistances != null) {
            for (var accGroup : collector.unalignedDistances.values()) {
                // TODO: get rid of collector key and just use the normal vector's hash code
                accGroup.prepareIntegration();
                distancesByNormal.put(accGroup.normal, accGroup.facePlaneDistances);
            }
        }

        VertexRange range = TranslucentData.getUnassignedVertexRange(translucentMesh);
        var buffer = new NativeBuffer(TranslucentData.quadCountToIndexBytes(quads.length));

        var dynamicData = new DynamicData(sectionPos, buffer, range, quads, collector, distancesByNormal);

        dynamicData.sort(cameraPos);

        return dynamicData;
    }
}
