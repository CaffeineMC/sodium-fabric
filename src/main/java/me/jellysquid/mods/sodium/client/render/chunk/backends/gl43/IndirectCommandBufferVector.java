package me.jellysquid.mods.sodium.client.render.chunk.backends.gl43;

import me.jellysquid.mods.sodium.client.render.chunk.multidraw.ChunkDrawCallBatcher;
import me.jellysquid.mods.sodium.client.render.chunk.multidraw.StructBuffer;
import org.lwjgl.system.MemoryUtil;

public class IndirectCommandBufferVector extends StructBuffer {
    protected IndirectCommandBufferVector(int capacity) {
        super(capacity, 16);
    }

    public static IndirectCommandBufferVector create(int capacity) {
        return new IndirectCommandBufferVector(capacity);
    }

    public void begin() {
        this.buffer.clear();
    }

    public void end() {
        this.buffer.flip();
    }

    public void pushCommandBuffer(ChunkDrawCallBatcher batcher) {
        int len = batcher.getArrayLength();

        if (this.buffer.remaining() < len) {
            this.growBuffer(len);
        }

        this.buffer.put(batcher.getBuffer());
    }

    protected void growBuffer(int n) {
        this.buffer = MemoryUtil.memRealloc(this.buffer, Math.max(this.buffer.capacity() * 2, this.buffer.capacity() + n));
    }
}
