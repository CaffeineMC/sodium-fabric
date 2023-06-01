package me.jellysquid.mods.sodium.client.render.chunk.graph;

import me.jellysquid.mods.sodium.client.render.chunk.RenderSection;
import net.minecraft.util.math.Direction;

import java.util.Arrays;

public class ChunkGraphIterationQueue {
    private RenderSection[] renders;

    private int pos;
    private int capacity;

    public ChunkGraphIterationQueue() {
        this(4096);
    }

    public ChunkGraphIterationQueue(int capacity) {
        this.renders = new RenderSection[capacity];

        this.capacity = capacity;
    }

    public void add(RenderSection render) {
        int i = this.pos++;

        if (i == this.capacity) {
            this.resize();
        }

        this.renders[i] = render;
    }

    private void resize() {
        this.capacity *= 2;

        this.renders = Arrays.copyOf(this.renders, this.capacity);
    }

    public RenderSection getRender(int i) {
        return this.renders[i];
    }

    public void clear() {
        this.pos = 0;
    }

    public int size() {
        return this.pos;
    }
}
