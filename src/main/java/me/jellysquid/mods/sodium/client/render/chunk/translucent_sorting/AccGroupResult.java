package me.jellysquid.mods.sodium.client.render.chunk.translucent_sorting;

import java.util.Collection;

import me.jellysquid.mods.sodium.client.model.quad.properties.ModelQuadFacing;

public abstract class AccGroupResult {
    AccumulationGroup[] alignedDistances;

    public AccumulationGroup[] getAlignedDistances() {
        return this.alignedDistances;
    }

    public AccumulationGroup[] getAlignedDistancesOrCreate() {
        if (this.alignedDistances == null) {
            this.alignedDistances = new AccumulationGroup[ModelQuadFacing.DIRECTIONS];
        }
        return this.alignedDistances;
    }

    public abstract Collection<AccumulationGroup> getUnalignedDistances();

    public abstract int getUnalignedDistanceCount();

    public abstract AccumulationGroup getGroupForUnalignedNormal(NormalList normalList);

    AccumulationGroup getGroupForNormal(NormalList normalList) {
        int collectorKey = normalList.getCollectorKey();
        if (collectorKey < 0xFF) {
            if (this.alignedDistances == null) {
                return null;
            }
            return this.alignedDistances[collectorKey];
        } else {
            return getGroupForUnalignedNormal(normalList);
        }
    }
}
