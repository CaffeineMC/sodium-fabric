package me.jellysquid.mods.sodium.client.model.vertex.buffer;

import me.jellysquid.mods.sodium.client.util.UnsafeUtil;
import net.minecraft.client.render.VertexFormat;
import org.lwjgl.system.MemoryUtil;
import sun.misc.Unsafe;

public abstract class VertexBufferWriterUnsafe extends VertexBufferWriter {
    protected static final Unsafe UNSAFE = UnsafeUtil.instanceNullable();

    protected long writePointer;

    protected VertexBufferWriterUnsafe(VertexBufferView backingBuffer, VertexFormat vertexFormat) {
        super(backingBuffer, vertexFormat);
    }

    @Override
    protected void onBufferStorageChanged() {
        this.writePointer = MemoryUtil.memAddress(this.backingBuffer.getDirectBuffer(), this.backingBuffer.getElementOffset());
    }

    protected void advance() {
        this.writePointer += this.vertexStride;
        this.vertexCount++;
    }
}
