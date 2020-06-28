package me.jellysquid.mods.sodium.client.render.chunk.multidraw;

import me.jellysquid.mods.sodium.client.util.UnsafeUtil;
import sun.misc.Unsafe;
import sun.nio.ch.DirectBuffer;

import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;

/**
 * Provides a fixed-size buffer which can be used to batch chunk section draw calls.
 */
public abstract class ChunkDrawCallBatcher extends BufferBuilder {
    private static final int STRUCT_SIZE = 16;

    protected final int capacity;

    protected ChunkDrawCallBatcher(int capacity) {
        super(capacity * STRUCT_SIZE);

        this.capacity = capacity;
    }

    public static ChunkDrawCallBatcher create(int capacity) {
        return USE_UNSAFE ? new UnsafeChunkDrawCallBatcher(capacity) : new NioChunkDrawCallBatcher(capacity);
    }

    public abstract void addIndirectDrawCall(int first, int count, int baseInstance, int instanceCount);

    public static class UnsafeChunkDrawCallBatcher extends ChunkDrawCallBatcher {
        private static final Unsafe UNSAFE = UnsafeUtil.instanceNullable();

        private final long basePointer;
        private long writePointer;

        public UnsafeChunkDrawCallBatcher(int capacity) {
            super(capacity);

            this.basePointer = ((DirectBuffer) this.buffer).address();
        }

        @Override
        public void begin() {
            super.begin();

            this.writePointer = this.basePointer;
        }

        @Override
        public void addIndirectDrawCall(int first, int count, int baseInstance, int instanceCount) {
            if (this.count++ >= this.capacity) {
                throw new BufferUnderflowException();
            }

            UNSAFE.putInt(this.writePointer     , count);         // Vertex Count
            UNSAFE.putInt(this.writePointer +  4, instanceCount); // Instance Count
            UNSAFE.putInt(this.writePointer +  8, first);         // Vertex Start
            UNSAFE.putInt(this.writePointer + 12, baseInstance);  // Base Instance

            this.writePointer += STRUCT_SIZE;
        }
    }

    public static class NioChunkDrawCallBatcher extends ChunkDrawCallBatcher {
        private int writeOffset;

        public NioChunkDrawCallBatcher(int capacity) {
            super(capacity);
        }

        @Override
        public void begin() {
            super.begin();

            this.writeOffset = 0;
        }

        @Override
        public void addIndirectDrawCall(int first, int count, int baseInstance, int instanceCount) {
            ByteBuffer buf = this.buffer;
            buf.putInt(this.writeOffset     , count);             // Vertex Count
            buf.putInt(this.writeOffset +  4, instanceCount);     // Instance Count
            buf.putInt(this.writeOffset +  8, first);             // Vertex Start
            buf.putInt(this.writeOffset + 12, baseInstance);      // Base Instance

            this.writeOffset += STRUCT_SIZE;
            this.count++;
        }
    }
}
