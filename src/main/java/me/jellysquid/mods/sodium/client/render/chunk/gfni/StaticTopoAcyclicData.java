package me.jellysquid.mods.sodium.client.render.chunk.gfni;

import me.jellysquid.mods.sodium.client.gl.util.VertexRange;
import me.jellysquid.mods.sodium.client.util.NativeBuffer;
import net.minecraft.util.math.ChunkSectionPos;

class StaticTopoAcyclicData extends TranslucentData {
    public final NativeBuffer buffer;
    public final VertexRange range;

    public StaticTopoAcyclicData(ChunkSectionPos sectionPos, NativeBuffer buffer, VertexRange range) {
        super(sectionPos);
        this.buffer = buffer;
        this.range = range;
    }

    @Override
    public SortType getSortType() {
        return SortType.STATIC_TOPO_ACYCLIC;
    }
}
