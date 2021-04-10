package me.jellysquid.mods.sodium.client.render.chunk.multidraw;

import me.jellysquid.mods.sodium.client.util.UnsafeUtil;
import org.lwjgl.system.MemoryUtil;
import sun.misc.Unsafe;

import java.nio.ByteBuffer;

/**
 * Provides a resizeable vector backed by native memory that can be used to build an array of chunk draw call
 * parameters.
 */
public abstract class ChunkDrawParamsVector extends StructBuffer {
    private static final int STRIDE = 16;

    protected ChunkDrawParamsVector(int capacity) {
        super(capacity, STRIDE);
    }

    public static ChunkDrawParamsVector create(int capacity) {
        return UnsafeUtil.isAvailable() ? new UnsafeChunkDrawCallVector(capacity) : new NioChunkDrawCallVector(capacity);
    }

    public abstract void pushChunkDrawParams(float x, float y, float z);

    public abstract void reset();

    protected void growBuffer() {
        this.buffer = MemoryUtil.memRealloc(this.buffer, this.buffer.capacity() * 2);
    }

    public static class UnsafeChunkDrawCallVector extends ChunkDrawParamsVector {
        private static final Unsafe UNSAFE = UnsafeUtil.instanceNullable();

        private long writeBase;
        private long writePointer;
        private long writeEnd;

        public UnsafeChunkDrawCallVector(int capacity) {
            super(capacity);

            this.updatePointers(0);
        }

        @Override
        @SuppressWarnings("SuspiciousNameCombination")
        public void pushChunkDrawParams(float x, float y, float z) {
            if (this.writePointer >= this.writeEnd) {
                this.growBuffer();
            }

            long l = this.writePointer;

            UNSAFE.putFloat(l    , x);
            UNSAFE.putFloat(l + 4, y);
            UNSAFE.putFloat(l + 8, z);

            this.writePointer += STRIDE;
        }

        @Override
        protected void growBuffer() {
            super.growBuffer();

            this.updatePointers(this.writePointer - this.writeBase);
        }

        @Override
        public void reset() {
            this.writePointer = this.writeBase;
        }

        private void updatePointers(long offset) {
            this.writeBase = MemoryUtil.memAddress(this.buffer);
            this.writeEnd = this.writeBase + this.buffer.capacity();

            this.writePointer = this.writeBase + offset;
        }
    }

    public static class NioChunkDrawCallVector extends ChunkDrawParamsVector {
        private int writeOffset;
        private int capacity;

        public NioChunkDrawCallVector(int capacity) {
            super(capacity);

            this.onBufferChanged();
        }

        @Override
        public void pushChunkDrawParams(float x, float y, float z) {
            if (this.writeOffset >= this.capacity) {
                this.growBuffer();
            }

            ByteBuffer buf = this.buffer;
            buf.putFloat(this.writeOffset    , x);
            buf.putFloat(this.writeOffset + 4, y);
            buf.putFloat(this.writeOffset + 8, z);

            this.writeOffset += STRIDE;
        }

        @Override
        protected void growBuffer() {
            super.growBuffer();

            this.onBufferChanged();
        }

        @Override
        public void reset() {
            this.writeOffset = 0;
        }

        private void onBufferChanged() {
            this.capacity = this.buffer.capacity();
        }
    }
}
