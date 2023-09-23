package me.jellysquid.mods.sodium.client.render.chunk.gfni;

import me.jellysquid.mods.sodium.client.gl.util.VertexRange;
import me.jellysquid.mods.sodium.client.render.chunk.data.BuiltSectionMeshParts;
import me.jellysquid.mods.sodium.client.util.NativeBuffer;
import net.minecraft.util.math.ChunkSectionPos;

public class StaticTopoAcyclicData extends MixedDirectionData {
    StaticTopoAcyclicData(ChunkSectionPos sectionPos, NativeBuffer buffer, VertexRange range) {
        super(sectionPos, buffer, range);
    }

    @Override
    public SortType getSortType() {
        return SortType.STATIC_TOPO_ACYCLIC;
    }

    static StaticTopoAcyclicData fromMesh(BuiltSectionMeshParts translucentMesh,
            TQuad[] quads, ChunkSectionPos sectionPos) {
        VertexRange range = TranslucentData.getUnassignedVertexRange(translucentMesh);
        var buffer = new NativeBuffer(TranslucentData.vertexCountToIndexBytes(range.vertexCount()));
        var indexBuffer = buffer.getDirectBuffer().asIntBuffer();

        ComplexSorting.topoSortAlignedAcyclicSafe(indexBuffer, quads, null);

        return new StaticTopoAcyclicData(sectionPos, buffer, range);
    }
}
