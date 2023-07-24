package me.jellysquid.mods.sodium.client.render.chunk.graph;

import me.jellysquid.mods.sodium.client.render.chunk.RenderSection;

import java.util.Arrays;

public class ChunkGraphIterationQueue {
    private RenderSection[] renders;
    private byte[] directions;

    private int pos;
    private int capacity;

    public ChunkGraphIterationQueue() {
        this(4096);
    }

    public ChunkGraphIterationQueue(int capacity) {
        this.renders = new RenderSection[capacity];
        this.directions = new byte[capacity];

        this.capacity = capacity;
    }

    public void add(RenderSection render, int direction) {
        int i = this.pos++;

        if (i == this.capacity) {
            this.resize();
        }

        this.renders[i] = render;
        this.directions[i] = (byte) direction;
    }

    private void resize() {
        this.capacity *= 2;

        this.renders = Arrays.copyOf(this.renders, this.capacity);
        this.directions = Arrays.copyOf(this.directions, this.capacity);
    }

    public RenderSection getRender(int i) {
        return this.renders[i];
    }

    public int getDirection(int index) {
        return this.directions[index];
    }

    public void clear() {
        this.pos = 0;
    }

    public int size() {
        return this.pos;
    }
}
