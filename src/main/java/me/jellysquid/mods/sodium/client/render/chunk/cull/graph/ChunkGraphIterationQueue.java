package me.jellysquid.mods.sodium.client.render.chunk.cull.graph;

import java.util.Arrays;

public class ChunkGraphIterationQueue {
    private ChunkGraphNode[] nodes;
    private byte[] directions;
    private byte[] cullingState;

    private int pos;
    private int capacity;

    public ChunkGraphIterationQueue() {
        this(4096);
    }

    public ChunkGraphIterationQueue(int capacity) {
        this.nodes = new ChunkGraphNode[capacity];
        this.directions = new byte[capacity];
        this.cullingState = new byte[capacity];

        this.capacity = capacity;
    }

    public void add(ChunkGraphNode node, int direction) {
        this.add(node, direction, 0);
    }

    public void add(ChunkGraphNode node, int direction, int cullingState) {
        int i = this.pos++;

        if (i == this.capacity) {
            this.resize();
        }

        this.nodes[i] = node;
        this.directions[i] = (byte) direction;
        this.cullingState[i] = (byte) (cullingState | (1 << direction));
    }

    private void resize() {
        this.capacity *= 2;

        this.nodes = Arrays.copyOf(this.nodes, this.capacity);
        this.directions = Arrays.copyOf(this.directions, this.capacity);
        this.cullingState = Arrays.copyOf(this.cullingState, this.capacity);
    }

    public ChunkGraphNode getNode(int i) {
        return this.nodes[i];
    }

    public int getDirection(int i) {
        return this.directions[i];
    }

    public int getCullingState(int i) {
        return this.cullingState[i];
    }

    public void clear() {
        this.pos = 0;
    }

    public int size() {
        return this.pos;
    }
}
