package me.jellysquid.mods.sodium.client.render.chunk.backends.gl43;

import me.jellysquid.mods.sodium.client.render.chunk.multidraw.ChunkDrawCallBatcher;
import me.jellysquid.mods.sodium.client.render.chunk.multidraw.StructBuffer;
import org.lwjgl.system.MemoryUtil;

import java.nio.Buffer;

public class IndirectCommandBufferVector extends StructBuffer {
    protected IndirectCommandBufferVector(int capacity) {
        super(capacity, 16);
    }

    public static IndirectCommandBufferVector create(int capacity) {
        return new IndirectCommandBufferVector(capacity);
    }

    public void begin() {
        ((Buffer) this.buffer).clear(); // Cast to Buffer to prevent exceptions running on Java 8 when sodium is compiled with Java 9+
    }

    public void end() {
        ((Buffer) this.buffer).flip(); // Cast to Buffer to prevent exceptions running on Java 8 when sodium is compiled with Java 9+
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
