package me.jellysquid.mods.sodium.client.render.chunk.gfni;

import me.jellysquid.mods.sodium.client.gl.util.VertexRange;
import me.jellysquid.mods.sodium.client.util.NativeBuffer;
import net.minecraft.util.math.ChunkSectionPos;

public abstract class SplitDirectionData extends PresentTranslucentData {
    public final VertexRange[] ranges;

    public SplitDirectionData(ChunkSectionPos sectionPos, NativeBuffer buffer, VertexRange[] ranges) {
        super(sectionPos, buffer);
        this.ranges = ranges;
    }

    @Override
    public VertexRange[] getVertexRanges() {
        return this.ranges;
    }
}
