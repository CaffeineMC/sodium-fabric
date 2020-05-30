package me.jellysquid.mods.sodium.client.gl.util;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;

/**
 * Provides a fixed-size queue for batching draw calls for vertex data in the same buffer. This internally
 * uses {@link GL14#glMultiDrawArrays(int, IntBuffer, IntBuffer)} and should be compatible on any relevant platform.
 */
public class MultiDrawBatch {
    private final IntBuffer bufIndices;
    private final IntBuffer bufLen;
    private final int capacity;
    private int count;

    public MultiDrawBatch(int capacity) {
        this.capacity = capacity;
        this.bufIndices = allocateIntBuffer(capacity);
        this.bufLen = allocateIntBuffer(capacity);
    }

    /**
     * Adds the given set of vertices to the draw queue.
     */
    public void add(int offset, int len) {
        if (this.count++ >= this.capacity) {
            throw new RuntimeException("Maximum batch size exceeded");
        }

        this.bufIndices.put(offset);
        this.bufLen.put(len);
    }

    /**
     * Performs a multi-draw with the given queue, clearing it afterwards.
     */
    public void draw() {
        this.bufIndices.flip();
        this.bufLen.flip();

        GL14.glMultiDrawArrays(GL11.GL_QUADS, this.bufIndices, this.bufLen);

        this.bufIndices.clear();
        this.bufLen.clear();

        this.count = 0;
    }
    
    private static IntBuffer allocateIntBuffer(int size) {
        return ByteBuffer.allocateDirect(size * 4)
                .order(ByteOrder.nativeOrder())
                .asIntBuffer();
    }

    public boolean isEmpty() {
        return this.count <= 0;
    }
}
