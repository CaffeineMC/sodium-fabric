package me.jellysquid.mods.sodium.render.chunk.arena.staging;

import me.jellysquid.mods.thingl.buffer.Buffer;
import me.jellysquid.mods.thingl.buffer.BufferUsage;
import me.jellysquid.mods.thingl.buffer.MutableBuffer;
import me.jellysquid.mods.thingl.device.RenderDevice;

import java.nio.ByteBuffer;

public class FallbackStagingBuffer implements StagingBuffer {
    private final RenderDevice device;
    private final MutableBuffer fallbackBufferObject;

    public FallbackStagingBuffer(RenderDevice device) {
        this.fallbackBufferObject = device.createMutableBuffer();
        this.device = device;
    }

    @Override
    public void enqueueCopy(ByteBuffer data, Buffer dst, long writeOffset) {
        this.device.uploadData(this.fallbackBufferObject, data, BufferUsage.STREAM_COPY);
        this.device.copyBufferSubData(this.fallbackBufferObject, dst, 0, writeOffset, data.remaining());
    }

    @Override
    public void flush() {
        this.device.allocateStorage(this.fallbackBufferObject, 0L, BufferUsage.STREAM_COPY);
    }

    @Override
    public void delete() {
        this.device.deleteBuffer(this.fallbackBufferObject);
    }

    @Override
    public void flip() {

    }

    @Override
    public String toString() {
        return "Fallback";
    }
}
