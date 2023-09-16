package me.jellysquid.mods.sodium.client.render.chunk.gfni;

import me.jellysquid.mods.sodium.client.gl.util.VertexRange;
import me.jellysquid.mods.sodium.client.render.chunk.data.BuiltSectionMeshParts;
import me.jellysquid.mods.sodium.client.util.NativeBuffer;
import net.minecraft.util.math.ChunkSectionPos;

public class StaticTopoAcyclicData extends MixedDirectionData {
    public StaticTopoAcyclicData(ChunkSectionPos sectionPos, NativeBuffer buffer, VertexRange range) {
        super(sectionPos, buffer, range);
    }

    @Override
    public SortType getSortType() {
        return SortType.STATIC_TOPO_ACYCLIC;
    }

    // NOTE: requires filling the contained buffer with data afterwards
    static StaticTopoAcyclicData fromMesh(BuiltSectionMeshParts translucentMesh, ChunkSectionPos sectionPos) {
        VertexRange range = TranslucentData.getUnassignedVertexRange(translucentMesh);
        var buffer = new NativeBuffer(TranslucentData.vertexCountToIndexBytes(range.vertexCount()));

        return new StaticTopoAcyclicData(sectionPos, buffer, range);
    }
}
