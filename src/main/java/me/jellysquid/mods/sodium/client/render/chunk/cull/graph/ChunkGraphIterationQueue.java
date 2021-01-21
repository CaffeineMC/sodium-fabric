package me.jellysquid.mods.sodium.client.render.chunk.cull.graph;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import net.minecraft.util.math.Direction;

import java.util.Arrays;

public class ChunkGraphIterationQueue {
    private int[] positions;
    private ChunkGraphNode[] nodes;
    private Direction[] directions;

    private int pos;
    private int capacity;

    public ChunkGraphIterationQueue() {
        this(4096);
    }

    public ChunkGraphIterationQueue(int capacity) {
        this.positions = new int[capacity];
        this.nodes = new ChunkGraphNode[capacity];
        this.directions = new Direction[capacity];

        this.capacity = capacity;
    }

    public void add(ChunkGraphNode node, Direction direction) {
        int i = this.pos++;

        if (i == this.capacity) {
            this.resize();
        }

        this.positions[i] = node.getId();
        this.nodes[i] = node;
        this.directions[i] = direction;
    }

    private void resize() {
        this.capacity *= 2;

        this.positions = Arrays.copyOf(this.positions, this.capacity);
        this.nodes = Arrays.copyOf(this.nodes, this.capacity);
        this.directions = Arrays.copyOf(this.directions, this.capacity);
    }

    public ChunkGraphNode getNode(int i) {
        return this.nodes[i];
    }

    public Direction getDirection(int i) {
        return this.directions[i];
    }

    public void clear() {
        this.pos = 0;
    }

    public int size() {
        return this.pos;
    }

    public IntArrayList getOrderedIdList() {
        return IntArrayList.wrap(this.positions, this.pos);
    }
}
