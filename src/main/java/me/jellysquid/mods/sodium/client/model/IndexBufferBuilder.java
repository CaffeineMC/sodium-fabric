package me.jellysquid.mods.sodium.client.model;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntIterator;

import java.nio.ByteBuffer;

public class IndexBufferBuilder {
    private final IntArrayList indices;

    public IndexBufferBuilder(int count) {
        this.indices = new IntArrayList(count);
    }

    public void add(int i) {
        this.indices.add(i);
    }

    public void get(ByteBuffer buffer) {
        IntIterator it = this.indices.iterator();

        while (it.hasNext()) {
            buffer.putInt(it.nextInt());
        }
    }

    public int getSize() {
        return this.getCount() * 4;
    }

    public void reset() {
        this.indices.clear();
    }

    public int getCount() {
        return this.indices.size();
    }
}
