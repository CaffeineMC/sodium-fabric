package me.jellysquid.mods.sodium.client.render.chunk.backend.multidraw;

import me.jellysquid.mods.sodium.client.gl.util.StructBuffer;
import me.jellysquid.mods.sodium.client.util.UnsafeUtil;

public interface ChunkDrawParamsBufferBuilder extends StructBuffer {
    int STRIDE = 16;

    static ChunkDrawParamsBufferBuilder create(int capacity) {
        return UnsafeUtil.isAvailable() ? new UnsafeChunkDrawParamsBufferBuilder(capacity) :
                new NioChunkDrawParamsBufferBuilder(capacity);
    }

    void pushChunkDrawParams(float x, float y, float z);

    class UnsafeChunkDrawParamsBufferBuilder extends StructBuffer.UnsafeStructBuffer
            implements ChunkDrawParamsBufferBuilder {

        protected UnsafeChunkDrawParamsBufferBuilder(int capacity) {
            super(ChunkDrawParamsBufferBuilder.STRIDE, capacity);
        }

        @Override
        public void pushChunkDrawParams(float x, float y, float z) {
            if (this.count++ >= this.capacity) {
                this.growBuffer();
            }

            long address = this.writePointer;

            UNSAFE.putFloat(address, x);
            UNSAFE.putFloat(address +  4, y);
            UNSAFE.putFloat(address +  8, z);

            this.writePointer += this.stride;
        }
    }

    class NioChunkDrawParamsBufferBuilder extends NioStructBuffer
            implements ChunkDrawParamsBufferBuilder {
        public NioChunkDrawParamsBufferBuilder(int capacity) {
            super(ChunkDrawParamsBufferBuilder.STRIDE, capacity);
        }

        @Override
        public void pushChunkDrawParams(float x, float y, float z) {
            if (this.buffer.remaining() < this.stride) {
                this.growBuffer();
            }

            int offset = this.buffer.position();

            this.buffer.putFloat(offset, x);
            this.buffer.putFloat(offset + 4, y);
            this.buffer.putFloat(offset + 8, z);

            this.buffer.position(offset + this.stride);
        }
    }
}
