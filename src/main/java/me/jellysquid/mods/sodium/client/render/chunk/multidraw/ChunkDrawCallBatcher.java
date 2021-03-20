package me.jellysquid.mods.sodium.client.render.chunk.multidraw;

import me.jellysquid.mods.sodium.client.util.UnsafeUtil;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.system.MemoryUtil;
import sun.misc.Unsafe;

import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;

/**
 * Provides a fixed-size buffer which can be used to batch chunk section draw calls.
 */
public abstract class ChunkDrawCallBatcher extends StructBuffer {
    private static final int STRIDE = 16;

    protected boolean isBuilding;

    protected int arrayLength;
    protected int count;

    protected ChunkDrawCallBatcher(int capacity) {
        super(MathHelper.smallestEncompassingPowerOfTwo(capacity), STRIDE);
    }

    public static ChunkDrawCallBatcher create(int capacity) {
        return UnsafeUtil.isAvailable() ? new UnsafeChunkDrawCallBatcher(capacity) : new NioChunkDrawCallBatcher(capacity);
    }

    public void begin() {
        this.isBuilding = true;
        this.count = 0;
        this.arrayLength = 0;

        this.buffer.limit(this.buffer.capacity());
    }

    public void end() {
        this.isBuilding = false;

        this.arrayLength = this.count * STRIDE;
        this.buffer.limit(this.arrayLength);
        this.buffer.position(0);
    }

    public boolean isBuilding() {
        return this.isBuilding;
    }

    public abstract void addIndirectDrawCall(int first, int count, int baseInstance, int instanceCount);

    public int getCount() {
        return this.count;
    }

    public static class UnsafeChunkDrawCallBatcher extends ChunkDrawCallBatcher {
        private static final Unsafe UNSAFE = UnsafeUtil.instanceNullable();

        private final long basePointer;

        private long writePointer;
        private long writeEnd;

        public UnsafeChunkDrawCallBatcher(int capacity) {
            super(capacity);

            this.basePointer = MemoryUtil.memAddress(this.buffer);
            this.writeEnd = this.basePointer + this.buffer.capacity();
        }

        @Override
        public void begin() {
            super.begin();

            this.writePointer = this.basePointer;
        }

        @Override
        public void end() {
            this.count = (int) (this.writePointer - this.basePointer) / STRIDE;

            super.end();
        }

        @Override
        public void addIndirectDrawCall(int first, int count, int baseInstance, int instanceCount) {
            long l = this.writePointer;

            if (l >= this.writeEnd) {
                throw new BufferUnderflowException();
            }

            UNSAFE.putInt(l     , count);         // Vertex Count
            UNSAFE.putInt(l +  4, instanceCount); // Instance Count
            UNSAFE.putInt(l +  8, first);         // Vertex Start
            UNSAFE.putInt(l + 12, baseInstance);  // Base Instance

            this.writePointer += STRIDE;
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
        public void end() {
            this.count = this.writeOffset / STRIDE;

            super.end();
        }

        @Override
        public void addIndirectDrawCall(int first, int count, int baseInstance, int instanceCount) {
            ByteBuffer buf = this.buffer;
            buf.putInt(this.writeOffset     , count);             // Vertex Count
            buf.putInt(this.writeOffset +  4, instanceCount);     // Instance Count
            buf.putInt(this.writeOffset +  8, first);             // Vertex Start
            buf.putInt(this.writeOffset + 12, baseInstance);      // Base Instance

            this.writeOffset += STRIDE;
        }
    }

    public int getArrayLength() {
        return this.arrayLength;
    }

}
