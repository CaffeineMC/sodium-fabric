package net.caffeinemc.sodium.render.chunk.state;

import net.caffeinemc.sodium.render.chunk.RenderSection;
import net.minecraft.util.math.Direction;

import java.util.Arrays;

public class ChunkGraphIterationQueue {
    private int[] renders;
    private int[] directions;

    private int pos;
    private int capacity;

    public ChunkGraphIterationQueue() {
        this(4096);
    }

    public ChunkGraphIterationQueue(int capacity) {
        this.renders = new int[capacity];
        this.directions = new int[capacity];

        this.capacity = capacity;
    }

    public void add(int render, int direction) {
        int i = this.pos++;

        if (i == this.capacity) {
            this.resize();
        }

        this.renders[i] = render;
        this.directions[i] = direction;
    }

    private void resize() {
        this.capacity *= 2;

        this.renders = Arrays.copyOf(this.renders, this.capacity);
        this.directions = Arrays.copyOf(this.directions, this.capacity);
    }

    public int getRender(int i) {
        return this.renders[i];
    }

    public int getDirection(int i) {
        return this.directions[i];
    }

    public void clear() {
        this.pos = 0;
    }

    public int size() {
        return this.pos;
    }
}
