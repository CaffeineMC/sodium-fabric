package me.jellysquid.mods.sodium.client.model;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntIterator;
import me.jellysquid.mods.sodium.client.model.quad.properties.ModelQuadWinding;

import java.nio.ByteBuffer;

public class IndexBufferBuilder {
    private final IntArrayList indices;

    public IndexBufferBuilder(int count) {
        this.indices = new IntArrayList(count);
    }

    public void add(int start, ModelQuadWinding winding) {
        for (int index : winding.getIndices()) {
            this.indices.add(start + index);
        }
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

    public void start() {
        this.indices.clear();
    }

    public int getCount() {
        return this.indices.size();
    }
}
