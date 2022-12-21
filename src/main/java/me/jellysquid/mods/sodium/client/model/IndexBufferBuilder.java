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

    private static GlIndexType getOptimalIndexType(int count) {
        if (count < 65536) {
            return GlIndexType.UNSIGNED_SHORT;
        } else {
            return GlIndexType.UNSIGNED_INT;
        }
    }

    public int getCount() {
        return this.indices.size();
    }

    public static class Result {
        private final IntArrayList indices;
        private final GlIndexType format;

        private Result(IntArrayList indices) {
            this.indices = indices;
            this.format = GlIndexType.UNSIGNED_INT;
        }

        public int writeTo(int offset, ByteBuffer buffer) {
            int pointer = offset;

            for (int i = 0; i < this.indices.size(); i++){
                buffer.putInt(pointer, this.indices.getInt(i));
                pointer += 4;
            }

            return pointer;
        }

        public int getByteSize() {
            return this.indices.size() * this.format.getStride();
        }

        public int getCount() {
            return this.indices.size();
        }

        public GlIndexType getFormat() {
            return this.format;
        }
    }
}
