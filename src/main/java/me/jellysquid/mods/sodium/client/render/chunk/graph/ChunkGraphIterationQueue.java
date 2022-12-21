package me.jellysquid.mods.sodium.client.render.chunk.graph;

import me.jellysquid.mods.sodium.client.render.chunk.RenderSection;
import net.minecraft.util.math.Direction;

import java.util.Arrays;

public class ChunkGraphIterationQueue {
    private int[] sections;

    private int pos;
    private int capacity;

    public ChunkGraphIterationQueue() {
        this(4096);
    }

    public ChunkGraphIterationQueue(int capacity) {
        this.sections = new int[capacity];

        this.capacity = capacity;
    }

    public void add(int section) {
        int i = this.pos++;

        if (i == this.capacity) {
            this.resize();
        }

        this.sections[i] = section;
    }

    private void resize() {
        this.capacity *= 2;
        this.sections = Arrays.copyOf(this.sections, this.capacity);
    }

    public int getSection(int i) {
        return this.sections[i];
    }

    public void clear() {
        this.pos = 0;
    }

    public int size() {
        return this.pos;
    }
}
