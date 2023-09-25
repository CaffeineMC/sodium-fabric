package me.jellysquid.mods.sodium.client.render.chunk.gfni;

import me.jellysquid.mods.sodium.client.gl.util.VertexRange;
import me.jellysquid.mods.sodium.client.model.quad.properties.ModelQuadFacing;
import me.jellysquid.mods.sodium.client.util.NativeBuffer;
import net.minecraft.util.math.ChunkSectionPos;

public abstract class MixedDirectionData extends PresentTranslucentData {
    public final VertexRange range;

    MixedDirectionData(ChunkSectionPos sectionPos, NativeBuffer buffer, VertexRange range) {
        super(sectionPos, buffer);
        this.range = range;
    }

    @Override
    public VertexRange[] getVertexRanges() {
        // TODO: cache this array?
        var ranges = new VertexRange[ModelQuadFacing.COUNT];
        ranges[ModelQuadFacing.UNASSIGNED.ordinal()] = this.range;
        return ranges;
    }
}
