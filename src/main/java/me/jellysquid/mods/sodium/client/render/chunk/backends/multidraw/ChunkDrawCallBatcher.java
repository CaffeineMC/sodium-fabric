package me.jellysquid.mods.sodium.client.render.chunk.backends.multidraw;

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
    protected final int capacity;

    protected boolean isBuilding;
    protected int count;

    protected int arrayLength;
    private int largestCount;

    protected ChunkDrawCallBatcher(int capacity) {
        super(MathHelper.smallestEncompassingPowerOfTwo(capacity), 20);

        this.capacity = capacity;
    }

    public static ChunkDrawCallBatcher create(int capacity) {
        return UnsafeUtil.isAvailable() ? new UnsafeChunkDrawCallBatcher(capacity) : new NioChunkDrawCallBatcher(capacity);
    }

    public void begin() {
        this.isBuilding = true;
        this.count = 0;
        this.arrayLength = 0;
        this.largestCount = 0;

        this.buffer.limit(this.buffer.capacity());
    }

    public void end() {
        this.isBuilding = false;

        this.arrayLength = this.count * this.stride;
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

    void updateCount(int c){
        if(c > this.largestCount){
            this.largestCount = c;
        }
    }

    public int getLargestCount() {
        return this.largestCount;
    }

    public static class UnsafeChunkDrawCallBatcher extends ChunkDrawCallBatcher {
        private static final Unsafe UNSAFE = UnsafeUtil.instanceNullable();

        private final long basePointer;
        private long writePointer;

        public UnsafeChunkDrawCallBatcher(int capacity) {
            super(capacity);

            this.basePointer = MemoryUtil.memAddress(this.buffer);
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

            int indexCount = count * 6 / 4; // Vertex Count -> Index count

            UNSAFE.putInt(this.writePointer     , indexCount);         // Index Count
            UNSAFE.putInt(this.writePointer +  4, instanceCount); // Instance Count
            UNSAFE.putInt(this.writePointer +  8, 0);         // Index Start
            UNSAFE.putInt(this.writePointer +  12, first);         // Base Vertex
            UNSAFE.putInt(this.writePointer + 16, baseInstance);  // Base Instance

            this.updateCount(indexCount);

            this.writePointer += this.stride;
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

            int indexCount = count * 6 / 4; // Vertex Count -> Index count

            buf.putInt(this.writeOffset     , indexCount);             // Index Count
            buf.putInt(this.writeOffset +  4, instanceCount);     // Instance Count
            buf.putInt(this.writeOffset +  8, 0);             // Index Start
            buf.putInt(this.writeOffset + 12, first);            // Base Vertex
            buf.putInt(this.writeOffset + 16, baseInstance);      // Base Instance

            this.updateCount(indexCount);

            this.writeOffset += this.stride;
            this.count++;
        }
    }

    public int getArrayLength() {
        return this.arrayLength;
    }

}
