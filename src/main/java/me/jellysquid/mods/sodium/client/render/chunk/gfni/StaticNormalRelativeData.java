package me.jellysquid.mods.sodium.client.render.chunk.gfni;

import me.jellysquid.mods.sodium.client.gl.util.VertexRange;
import me.jellysquid.mods.sodium.client.util.NativeBuffer;
import net.minecraft.util.math.ChunkSectionPos;

class StaticNormalRelativeData extends TranslucentData {
    public final NativeBuffer buffer;
    public final VertexRange[] ranges;

    public StaticNormalRelativeData(ChunkSectionPos sectionPos, NativeBuffer buffer, VertexRange[] ranges) {
        super(sectionPos);
        this.buffer = buffer;
        this.ranges = ranges;
    }

    @Override
    public SortType getSortType() {
        return SortType.STATIC_NORMAL_RELATIVE;
    }
}
