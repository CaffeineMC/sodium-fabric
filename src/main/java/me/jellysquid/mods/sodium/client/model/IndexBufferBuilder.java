package me.jellysquid.mods.sodium.client.model;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntIterator;
import me.jellysquid.mods.sodium.client.gl.tessellation.GlIndexType;
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

    public void start() {
        this.indices.clear();
    }

    public Result pop() {
        if (this.indices.isEmpty()) {
            return null;
        }

        return new Result(this.indices);
    }

    public int getCount() {
        return this.indices.size();
    }

    public static class Result {
        private final IntArrayList indices;
        private final GlIndexType format = GlIndexType.UNSIGNED_INT;

        private Result(IntArrayList indices) {
            this.indices = indices;
        }

        public int writeTo(int offset, ByteBuffer buffer) {
            IntIterator it = this.indices.iterator();
            int stride = this.format.getStride();

            int pointer = offset;

            while (it.hasNext()) {
                buffer.putInt(pointer, it.nextInt());
                pointer += stride;
            }

            return pointer;
        }

        public int getByteSize() {
            return this.indices.size() * this.format.getStride();
        }

        public int getCount() {
            return this.indices.size();
        }

        public int getBaseVertex() {
            return 0;
        }

        public GlIndexType getFormat() {
            return this.format;
        }
    }
}
