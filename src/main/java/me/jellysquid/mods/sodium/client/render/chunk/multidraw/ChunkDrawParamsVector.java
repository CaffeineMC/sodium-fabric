package me.jellysquid.mods.sodium.client.render.chunk.multidraw;

import me.jellysquid.mods.sodium.client.util.UnsafeUtil;
import org.lwjgl.system.MemoryUtil;
import sun.misc.Unsafe;
import sun.nio.ch.DirectBuffer;

import java.nio.ByteBuffer;

/**
 * Provides a resizeable vector backed by native memory that can be used to build an array of chunk draw call
 * parameters.
 */
public abstract class ChunkDrawParamsVector extends BufferBuilder {
    private static final int VEC4_SIZE = 16;

    protected int capacity;

    protected ChunkDrawParamsVector(int capacity) {
        super(capacity * VEC4_SIZE);

        this.capacity = capacity;
    }

    public static ChunkDrawParamsVector create(int capacity) {
        return USE_UNSAFE ? new UnsafeChunkDrawCallVector(capacity) : new NioChunkDrawCallVector(capacity);
    }

    public abstract void pushChunkDrawParams(float x, float y, float z);

    protected void growBuffer() {
        this.capacity = this.buffer.capacity() * 2;
        this.buffer = MemoryUtil.memRealloc(this.buffer, this.capacity);
    }

    public static class UnsafeChunkDrawCallVector extends ChunkDrawParamsVector {
        private static final Unsafe UNSAFE = UnsafeUtil.instanceNullable();

        private long basePointer;
        private long writePointer;

        public UnsafeChunkDrawCallVector(int capacity) {
            super(capacity);

            this.basePointer = ((DirectBuffer) this.buffer).address();
        }

        @Override
        @SuppressWarnings("SuspiciousNameCombination")
        public void pushChunkDrawParams(float x, float y, float z) {
            if (this.count++ >= this.capacity) {
                this.growBuffer();
            }

            UNSAFE.putFloat(this.writePointer    , x);
            UNSAFE.putFloat(this.writePointer + 4, y);
            UNSAFE.putFloat(this.writePointer + 8, z);

            this.writePointer += VEC4_SIZE;
        }

        @Override
        protected void growBuffer() {
            super.growBuffer();

            long offset = this.writePointer - this.basePointer;
            this.basePointer = ((DirectBuffer) this.buffer).address();
            this.writePointer = this.basePointer + offset;
        }

        @Override
        public void begin() {
            super.begin();

            this.writePointer = this.basePointer;
        }
    }

    public static class NioChunkDrawCallVector extends ChunkDrawParamsVector {
        private int writeOffset;

        public NioChunkDrawCallVector(int capacity) {
            super(capacity);
        }

        @Override
        public void pushChunkDrawParams(float x, float y, float z) {
            if (this.count++ >= this.capacity) {
                this.growBuffer();
            }

            ByteBuffer buf = this.buffer;
            buf.putFloat(this.writeOffset    , x);
            buf.putFloat(this.writeOffset + 4, y);
            buf.putFloat(this.writeOffset + 8, z);

            this.writeOffset += VEC4_SIZE;
        }

        @Override
        public void begin() {
            super.begin();

            this.writeOffset = 0;
        }
    }
}
