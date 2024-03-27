package net.caffeinemc.mods.sodium.client.render.chunk.translucent_sorting.data;

import net.caffeinemc.mods.sodium.client.gl.util.VertexRange;
import net.caffeinemc.mods.sodium.client.util.NativeBuffer;
import net.minecraft.core.SectionPos;

/**
 * Super class for translucent data that is rendered separately for each facing.
 * (block face culling is possible) It's important that the indices are inserted
 * starting at zero for each facing.
 */
public abstract class SplitDirectionData extends PresentTranslucentData {
    private final VertexRange[] ranges;

    public SplitDirectionData(SectionPos sectionPos, VertexRange[] ranges, int quadCount) {
        super(sectionPos, quadCount);
        this.ranges = ranges;
    }

    @Override
    public VertexRange[] getVertexRanges() {
        return this.ranges;
    }
}
