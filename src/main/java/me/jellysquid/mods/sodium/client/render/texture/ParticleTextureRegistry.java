package me.jellysquid.mods.sodium.client.render.texture;

import it.unimi.dsi.fastutil.floats.FloatFloatImmutablePair;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

public class ParticleTextureRegistry {
    private final Object2IntOpenHashMap<FloatFloatImmutablePair> uvToIndex = new Object2IntOpenHashMap<>();

    private ArrayList<FloatFloatImmutablePair> toAdd = new ArrayList<>();

    private int currentIndex = 0;

    public int get(float u, float v) {
        FloatFloatImmutablePair key = new FloatFloatImmutablePair(u, v);
        return uvToIndex.computeIfAbsent(key, pairKey -> {
            toAdd.add((FloatFloatImmutablePair) pairKey);
            return currentIndex++;
        });
    }

    public boolean isDirty() {
        return !toAdd.isEmpty();
    }

    public List<FloatFloatImmutablePair> drainUpdates() {
        var ret = this.toAdd;
        this.toAdd = new ArrayList<>();
        return ret;
    }
}
