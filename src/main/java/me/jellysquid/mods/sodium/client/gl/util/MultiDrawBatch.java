package me.jellysquid.mods.sodium.client.gl.util;

import net.minecraft.client.util.GlAllocationUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;

import java.nio.IntBuffer;

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

    public void reset() {
        this.bufIndices.clear();
        this.bufLen.clear();
        this.count = 0;
    }

    public void add(int offset, int len) {
        if (this.count++ >= this.capacity) {
            throw new RuntimeException("Maximum batch size exceeded");
        }

        this.bufIndices.put(offset);
        this.bufLen.put(len);
    }

    public void draw() {
        this.bufIndices.flip();
        this.bufLen.flip();

        GL14.glMultiDrawArrays(GL11.GL_QUADS, this.bufIndices, this.bufLen);

        this.reset();
    }
    
    private static IntBuffer allocateIntBuffer(int size) {
        return GlAllocationUtils.allocateByteBuffer(size * 4).asIntBuffer();
    }

    public boolean isEmpty() {
        return this.count <= 0;
    }
}
