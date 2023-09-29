package me.jellysquid.mods.sodium.client.render.texture;

import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.caffeinemc.mods.sodium.api.util.RawUVs;

import java.util.ArrayList;
import java.util.List;

public class ParticleTextureRegistry {
    private final Object2IntOpenHashMap<RawUVs> uvToIndex = new Object2IntOpenHashMap<>();

    private ArrayList<RawUVs> toAdd = new ArrayList<>();

    private int currentIndex = 0;

    public int get(RawUVs uvs) {
        return uvToIndex.computeIfAbsent(uvs, pairKey -> {
            toAdd.add((RawUVs) pairKey);
            return currentIndex++;
        });
    }

    public boolean isDirty() {
        return !toAdd.isEmpty();
    }

    public List<RawUVs> drainUpdates() {
        var ret = this.toAdd;
        this.toAdd = new ArrayList<>();
        return ret;
    }
}
