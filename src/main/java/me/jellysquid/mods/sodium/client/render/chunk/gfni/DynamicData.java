package me.jellysquid.mods.sodium.client.render.chunk.gfni;

import java.util.List;

import org.joml.Vector3f;
import org.joml.Vector3fc;

import it.unimi.dsi.fastutil.ints.Int2ReferenceLinkedOpenHashMap;
import me.jellysquid.mods.sodium.client.gl.util.VertexRange;
import me.jellysquid.mods.sodium.client.model.quad.properties.ModelQuadFacing;
import me.jellysquid.mods.sodium.client.render.chunk.data.BuiltSectionMeshParts;
import me.jellysquid.mods.sodium.client.render.chunk.gfni.GroupBuilder.Quad;
import me.jellysquid.mods.sodium.client.util.NativeBuffer;
import me.jellysquid.mods.sodium.client.util.sorting.VertexSorters;
import net.minecraft.util.math.ChunkSectionPos;

public class DynamicData extends MixedDirectionData {
    public final Vector3f[] centers;
    private AccumulationGroup[] axisAlignedDistances;
    private Int2ReferenceLinkedOpenHashMap<AccumulationGroup> unalignedDistances;

    public DynamicData(ChunkSectionPos sectionPos,
            NativeBuffer buffer, VertexRange range, Vector3f[] centers,
            AccumulationGroup[] axisAlignedDistances,
            Int2ReferenceLinkedOpenHashMap<AccumulationGroup> unalignedDistances) {
        super(sectionPos, buffer, range);
        this.centers = centers;
        this.axisAlignedDistances = axisAlignedDistances;
        this.unalignedDistances = unalignedDistances;
    }

    @Override
    public SortType getSortType() {
        return SortType.DYNAMIC_ALL;
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

    public AccumulationGroup[] getAxisAlignedDistances() {
        return this.axisAlignedDistances;
    }

    public Int2ReferenceLinkedOpenHashMap<AccumulationGroup> getUnalignedDistances() {
        return this.unalignedDistances;
    }

    public void clearTriggerData() {
        this.axisAlignedDistances = null;
        this.unalignedDistances = null;
    }

    @Override
    public void sort(Vector3fc cameraPos) {
        var intBuffer = this.buffer.getDirectBuffer().asIntBuffer();
        TranslucentData.writeVertexIndexes(intBuffer,
                VertexSorters.sortByDistance(new Vector3f(cameraPos)).sort(this.centers));
    }

    static DynamicData fromMesh(BuiltSectionMeshParts translucentMesh, NativeBuffer reuseBuffer,
            Vector3fc cameraPos, List<Quad> quads, ChunkSectionPos sectionPos, GroupBuilder groupBuilder) {
        VertexRange range = TranslucentData.getUnassignedVertexRange(translucentMesh);
        int[] centerCounters = new int[ModelQuadFacing.COUNT];

        for (Quad quad : quads) {
            centerCounters[quad.facing().ordinal()]++;
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

        for (int i = 0; i < quads.size(); i++) {
            Quad quad = quads.get(i);
            centers[centerCounters[quad.facing().ordinal()]++] = quad.center();
        }

        var dynamicData = new DynamicData(sectionPos,
                reuseBuffer, range, centers, groupBuilder.axisAlignedDistances, groupBuilder.unalignedDistances);
        dynamicData.sort(cameraPos);
        return dynamicData;
    }
}
