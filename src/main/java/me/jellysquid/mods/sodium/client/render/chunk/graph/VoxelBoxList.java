package me.jellysquid.mods.sodium.client.render.chunk.graph;

import net.minecraft.util.math.ChunkSectionPos;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;

public class VoxelBoxList {
    public final ByteBuffer buffer;
    public final int count;

    public VoxelBoxList(ByteBuffer buffer, int count) {
        this.buffer = buffer;
        this.count = count;
    }

    public static class Builder {
        private static final int STRIDE = 12;

        private ByteBuffer buffer;
        private int count;
        private int capacity;

        public Builder() {
            this.capacity = 64;
            this.buffer = MemoryUtil.memAlloc(STRIDE * this.capacity);
        }

        public void add(int x1, int y1, int z1, int x2, int y2, int z2, int flags) {
            if (this.count >= this.capacity) {
                this.buffer = MemoryUtil.memRealloc(this.buffer, STRIDE * (this.capacity *= 2));
            }

            long ptr = MemoryUtil.memAddress(this.buffer, this.count++ * STRIDE);

            MemoryUtil.memPutByte(ptr + 0, (byte) x1);
            MemoryUtil.memPutByte(ptr + 1, (byte) y1);
            MemoryUtil.memPutByte(ptr + 2, (byte) z1);

            MemoryUtil.memPutByte(ptr + 4, (byte) x2);
            MemoryUtil.memPutByte(ptr + 5, (byte) y2);
            MemoryUtil.memPutByte(ptr + 6, (byte) z2);

            MemoryUtil.memPutInt(ptr + 8, flags);
        }

        public VoxelBoxList finish() {
            return new VoxelBoxList(MemoryUtil.memRealloc(this.buffer, this.count * STRIDE), this.count);
        }
    }
}
