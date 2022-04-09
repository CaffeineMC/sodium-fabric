package net.caffeinemc.sodium.render.chunk.state;

import it.unimi.dsi.fastutil.Arrays;

import java.util.NoSuchElementException;

public class ChunkGraphIterationQueue {
    private int[] renders;
    private int[] directions;

    private int start, end;
    private int length;

    public ChunkGraphIterationQueue() {
        this(256);
    }

    public ChunkGraphIterationQueue(int capacity) {
        this.renders = new int[capacity];
        this.directions = new int[capacity];

        this.length = capacity;
    }

    public void enqueue(int render, int direction) {
        var pos = this.end++;

        this.renders[pos] = render;
        this.directions[pos] = direction;

        if (this.end == this.length) {
            this.end = 0;
        }

        if (this.end == this.start) {
            this.expand();
        }
    }

    private void expand() {
        this.resize(this.length, (int) Math.min(Arrays.MAX_ARRAY_SIZE, 2L * this.length));
    }

    private void resize(final int size, final int newLength) {
        final int[] newRenders = new int[newLength];
        final int[] newDirections = new int[newLength];

        if (this.start >= this.end) {
            if (size != 0) {
                System.arraycopy(this.renders, this.start, newRenders, 0, this.length - this.start);
                System.arraycopy(this.renders, 0, newRenders, this.length - this.start, this.end);

                System.arraycopy(this.directions, this.start, newDirections, 0, this.length - this.start);
                System.arraycopy(this.directions, 0, newDirections, this.length - this.start, this.end);
            }
        } else {
            System.arraycopy(this.renders, this.start, newRenders, 0, this.end - this.start);
            System.arraycopy(this.directions, this.start, newDirections, 0, this.end - this.start);
        }

        this.start = 0;
        this.end = size;

        this.renders = newRenders;
        this.directions = newDirections;

        this.length = newLength;
    }

    public int dequeIndex() {
        if (this.start == this.end) {
            throw new NoSuchElementException();
        }

        final int index = this.start;

        if (++this.start == this.length) {
            this.start = 0;
        }

        return index;
    }

    public int getSectionId(int index) {
        return this.renders[index];
    }

    public int getDirection(int index) {
        return this.directions[index];
    }

    public int size() {
        final int apparentLength = this.end - this.start;
        return apparentLength >= 0 ? apparentLength : this.length + apparentLength;
    }

    public boolean isEmpty() {
        return this.size() == 0;
    }
}
