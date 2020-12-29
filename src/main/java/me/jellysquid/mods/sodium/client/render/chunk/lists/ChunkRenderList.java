package me.jellysquid.mods.sodium.client.render.chunk.lists;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import java.util.Arrays;

/**
 * A simple extension over {@link ObjectArrayList} which provides iterator methods in either FIFO or LIFO ordering.
 */
public class ChunkRenderList {
    private int[] stateArray;
    private int[] cullArray;
    private int size, capacity;

    public ChunkRenderList() {
        this(1024);
    }

    public ChunkRenderList(int capacity) {
        this.size = 0;
        this.capacity = capacity;

        this.stateArray = new int[capacity];
        this.cullArray = new int[capacity];
    }

    private void resize() {
        this.capacity = this.capacity * 2;

        this.stateArray = Arrays.copyOf(this.stateArray, this.capacity);
        this.cullArray = Arrays.copyOf(this.cullArray, this.capacity);
    }

    public void add(int state, int cull) {
        int idx = this.size++;

        if (idx >= this.capacity) {
            this.resize();
        }

        this.stateArray[idx] = state;
        this.cullArray[idx] = cull;
    }

    public void reset() {
        if (this.size == 0) {
            return;
        }

        for (int i = 0; i < this.size; i++) {
            this.stateArray[i] = -1;
        }

        for (int i = 0; i < this.size; i++) {
            this.cullArray[i] = 0;
        }

        this.size = 0;
    }

    /**
     * @return An iterator which returns elements in FIFO order or LIFO order if {@param backwards} is set
     */
    public ChunkRenderListIterator iterator(boolean backwards) {
        if (backwards) {
            return new ChunkRenderListIterator() {
                private int pos = ChunkRenderList.this.size - 1;

                @Override
                public int getGraphicsStateId() {
                    return ChunkRenderList.this.stateArray[this.pos];
                }

                @Override
                public int getVisibleFaces() {
                    return ChunkRenderList.this.cullArray[this.pos];
                }

                @Override
                public boolean hasNext() {
                    return this.pos >= 0;
                }

                @Override
                public void advance() {
                    this.pos--;
                }
            };
        } else {
            return new ChunkRenderListIterator() {
                private final int lim = ChunkRenderList.this.size;

                private int pos = 0;

                @Override
                public int getGraphicsStateId() {
                    return ChunkRenderList.this.stateArray[this.pos];
                }

                @Override
                public int getVisibleFaces() {
                    return ChunkRenderList.this.cullArray[this.pos];
                }

                @Override
                public boolean hasNext() {
                    return this.pos < this.lim;
                }

                @Override
                public void advance() {
                    this.pos++;
                }
            };
        }
    }
}
