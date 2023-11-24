package me.jellysquid.mods.sodium.client.render.chunk.translucent_sorting.bsp_tree;

import java.util.Collection;

import org.joml.Vector3fc;

import it.unimi.dsi.fastutil.objects.Object2ReferenceOpenHashMap;
import me.jellysquid.mods.sodium.client.render.chunk.translucent_sorting.AccGroupResult;
import me.jellysquid.mods.sodium.client.render.chunk.translucent_sorting.AccumulationGroup;
import me.jellysquid.mods.sodium.client.render.chunk.translucent_sorting.NormalList;

public class BSPResult extends AccGroupResult {
    Object2ReferenceOpenHashMap<Vector3fc, AccumulationGroup> unalignedDistances;

    BSPNode rootNode;

    @Override
    public Collection<AccumulationGroup> getUnalignedDistances() {
        if (this.unalignedDistances == null) {
            return null;
        }
        return this.unalignedDistances.values();
    }

    @Override
    public AccumulationGroup getGroupForUnalignedNormal(NormalList normalList) {
        if (this.unalignedDistances == null) {
            return null;
        }
        return this.unalignedDistances.get(normalList.getNormal());
    }

    @Override
    public int getUnalignedDistanceCount() {
        if (this.unalignedDistances == null) {
            return 0;
        }
        return this.unalignedDistances.size();
    }

    public BSPNode getRootNode() {
        return this.rootNode;
    }

}
