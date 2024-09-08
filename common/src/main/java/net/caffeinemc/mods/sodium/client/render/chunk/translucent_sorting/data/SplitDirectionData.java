package net.caffeinemc.mods.sodium.client.render.chunk.translucent_sorting.data;

import net.minecraft.core.SectionPos;

/**
 * Super class for translucent data that is rendered separately for each facing.
 * (block face culling is possible) It's important that the indices are inserted
 * starting at zero for each facing.
 */
public abstract class SplitDirectionData extends PresentTranslucentData {
    private final int[] vertexCounts;

    public SplitDirectionData(SectionPos sectionPos, int[] vertexCounts, int quadCount) {
        super(sectionPos, quadCount);
        this.vertexCounts = vertexCounts;
    }

    @Override
    public int[] getVertexCounts() {
        return this.vertexCounts;
    }
}
