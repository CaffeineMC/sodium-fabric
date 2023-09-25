package me.jellysquid.mods.sodium.client.render.chunk.gfni;

import java.nio.IntBuffer;

import org.joml.Vector3fc;

import it.unimi.dsi.fastutil.ints.Int2ReferenceLinkedOpenHashMap;
import me.jellysquid.mods.sodium.client.gl.util.VertexRange;
import me.jellysquid.mods.sodium.client.render.chunk.data.BuiltSectionMeshParts;
import me.jellysquid.mods.sodium.client.util.NativeBuffer;
import net.minecraft.util.math.ChunkSectionPos;

/**
 * TODO: the sort algorithm uses a heuristic to avoid issues arising from
 * parallel planes of many quads. (see below) This heuristic is imperfect and
 * can cause some artifacts. A better approach would be a visible-only
 * topological sort. That's hard though, because it needs to handle arbitrary
 * amounts of (potentially unaligned) quads and run on every trigger event.
 * Possibly re-use of (incrementally constructed) graph structures is necessary?
 */
public class DynamicData extends MixedDirectionData {
    private final TQuad[] quads;
    private final AccumulationGroup[] axisAlignedDistances;
    private final Int2ReferenceLinkedOpenHashMap<AccumulationGroup> unalignedDistances;

    DynamicData(ChunkSectionPos sectionPos,
            NativeBuffer buffer, VertexRange range, TQuad[] quads,
            AccumulationGroup[] axisAlignedDistances,
            Int2ReferenceLinkedOpenHashMap<AccumulationGroup> unalignedDistances) {
        super(sectionPos, buffer, range);
        this.quads = quads;
        this.axisAlignedDistances = axisAlignedDistances;
        this.unalignedDistances = unalignedDistances;
    }

    @Override
    public SortType getSortType() {
        return SortType.DYNAMIC_ALL;
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

    public AccumulationGroup[] getAxisAlignedDistances() {
        return this.axisAlignedDistances;
    }

    public Int2ReferenceLinkedOpenHashMap<AccumulationGroup> getUnalignedDistances() {
        return this.unalignedDistances;
    }

    @Override
    public void sort(Vector3fc cameraPos) {
        IntBuffer indexBuffer = this.buffer.getDirectBuffer().asIntBuffer();

        if (!ComplexSorting.topoSortFullGraphAcyclic(indexBuffer, this.quads, cameraPos)) {
            // if a camera position is given, no cycle should be possible
            throw new IllegalStateException("Failed to sort");
        }
    }

    static DynamicData fromMesh(BuiltSectionMeshParts translucentMesh,
            Vector3fc cameraPos, TQuad[] quads, ChunkSectionPos sectionPos, TranslucentGeometryCollector collector) {
        VertexRange range = TranslucentData.getUnassignedVertexRange(translucentMesh);
        var buffer = new NativeBuffer(TranslucentData.quadCountToIndexBytes(quads.length));

        var dynamicData = new DynamicData(sectionPos,
                buffer, range, quads,
                collector.axisAlignedDistances, collector.unalignedDistances);

        dynamicData.sort(cameraPos);

        return dynamicData;
    }
}
