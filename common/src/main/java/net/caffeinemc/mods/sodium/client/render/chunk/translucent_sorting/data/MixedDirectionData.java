package net.caffeinemc.mods.sodium.client.render.chunk.translucent_sorting.data;

import net.caffeinemc.mods.sodium.client.model.quad.properties.ModelQuadFacing;
import net.minecraft.core.SectionPos;

public abstract class MixedDirectionData extends PresentTranslucentData {
    private final int[] vertexCounts = new int[ModelQuadFacing.COUNT];

    MixedDirectionData(SectionPos sectionPos, int vertexCount, int quadCount) {
        super(sectionPos, quadCount);
        this.vertexCounts[ModelQuadFacing.UNASSIGNED.ordinal()] = vertexCount;
    }

    @Override
    public int[] getVertexCounts() {
        return this.vertexCounts;
    }
}
