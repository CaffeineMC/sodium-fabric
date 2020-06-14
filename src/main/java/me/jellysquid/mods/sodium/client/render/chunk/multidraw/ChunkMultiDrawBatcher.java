package me.jellysquid.mods.sodium.client.render.chunk.multidraw;

import org.lwjgl.opengl.GL14;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

/**
 * Provides a fixed-size queue for batching draw calls for vertex data in the same buffer. This internally
 * uses {@link GL14#glMultiDrawArrays(int, IntBuffer, IntBuffer)} and should be compatible on any relevant platform.
 */
public class ChunkMultiDrawBatcher {
    private final FloatBuffer bufOffsets;
    private final IntBuffer bufIndices;
    private final IntBuffer bufLen;
    private int count;
    private int readIndex;
    private boolean isBuilding;

    public ChunkMultiDrawBatcher(int capacity) {
        this.bufIndices = allocateByteBuffer(capacity * 4).asIntBuffer();
        this.bufLen = allocateByteBuffer(capacity * 4).asIntBuffer();
        this.bufOffsets = allocateByteBuffer(capacity * 16).asFloatBuffer();
    }

    private static ByteBuffer allocateByteBuffer(int size) {
        return ByteBuffer.allocateDirect(size)
                .order(ByteOrder.nativeOrder());
    }

    public FloatBuffer getUniformUploadBuffer() {
        return this.bufOffsets;
    }

    public IntBuffer getIndicesBuffer() {
        return this.bufIndices;
    }

    public IntBuffer getLengthBuffer() {
        return this.bufLen;
    }

    public void begin() {
        this.bufIndices.clear();
        this.bufLen.clear();
        this.bufOffsets.clear();
        this.count = 0;
        this.readIndex = 0;

        this.isBuilding = true;
    }

    public boolean getNextBatch(int maxBatchSize) {
        if (this.readIndex >= this.count) {
            return false;
        }

        int start = this.readIndex;
        int count = Math.min(maxBatchSize, this.count - this.readIndex);

        this.readIndex += count;

        int end = start + count;

        this.bufIndices.position(start);
        this.bufIndices.limit(end);

        this.bufLen.position(start);
        this.bufLen.limit(end);

        this.bufOffsets.position(start * 4);
        this.bufOffsets.limit(end * 4);

        return true;
    }

    public void end() {
        this.isBuilding = false;
    }

    public boolean isEmpty() {
        return this.count <= 0;
    }

    public void addChunkRender(int first, int count, float x, float y, float z) {
        int i = this.count++;
        this.bufIndices.put(i, first);
        this.bufLen.put(i, count);

        int j = i * 4;
        this.bufOffsets.put(j++, x);
        this.bufOffsets.put(j++, y);
        this.bufOffsets.put(j  , z);
    }

    public boolean isBuilding() {
        return this.isBuilding;
    }
}
