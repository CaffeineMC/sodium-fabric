package me.jellysquid.mods.sodium.client.render.particle.cache;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntCollection;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;

import java.util.Arrays;

public class TextureUsageMonitor {
    private static final int DEFAULT_INITIAL_CAPACITY = 64;

    private static final byte REMOVE_AFTER = 4;

    private byte[] lastUsed;

    public TextureUsageMonitor(int initialCapacity) {
        this.lastUsed = new byte[initialCapacity];
        Arrays.fill(lastUsed, (byte) -1);
    }

    public TextureUsageMonitor() {
        this(DEFAULT_INITIAL_CAPACITY);
    }

    public void markUsed(int index) {
        ensureCapacity(index);
        lastUsed[index] = 0;
    }

    /**
     * @return a List of the index of the UVs that should be removed
     */
    public IntList update() {
        IntList toRemove = new IntArrayList();
        for (int i = 0; i < lastUsed.length; ++i) {
            byte v = lastUsed[i];
            if (v < 0) continue;

            byte framesUnused = ++lastUsed[i];
            if (framesUnused > REMOVE_AFTER) {
                lastUsed[i] = -1;
                toRemove.add(i);
            }
        }

        return toRemove;
    }

    private void ensureCapacity(int high) {
        if (high >= this.lastUsed.length) reallocLastUsed(high);
    }

    private void reallocLastUsed(int high) {
        int newCapacity = this.lastUsed.length;
        while (high >= newCapacity) {
            newCapacity += (newCapacity >> 1);
        }
        byte[] newArray = new byte[newCapacity];
        System.arraycopy(lastUsed, 0, newArray, 0, lastUsed.length);
        Arrays.fill(newArray, lastUsed.length, newCapacity, (byte) -1);
        this.lastUsed = newArray;
    }
}
