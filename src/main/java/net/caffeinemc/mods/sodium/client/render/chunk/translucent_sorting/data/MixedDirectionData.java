package net.caffeinemc.mods.sodium.client.render.chunk.translucent_sorting.data;

import net.caffeinemc.mods.sodium.client.gl.util.VertexRange;
import net.caffeinemc.mods.sodium.client.model.quad.properties.ModelQuadFacing;
import net.caffeinemc.mods.sodium.client.util.NativeBuffer;
import net.minecraft.core.SectionPos;

public abstract class MixedDirectionData extends PresentTranslucentData {
    private final VertexRange[] ranges = new VertexRange[ModelQuadFacing.COUNT];

    MixedDirectionData(SectionPos sectionPos, VertexRange range, int quadCount) {
        super(sectionPos, quadCount);
        this.ranges[ModelQuadFacing.UNASSIGNED.ordinal()] = range;
    }

    @Override
    public VertexRange[] getVertexRanges() {
        return this.ranges;
    }
}
