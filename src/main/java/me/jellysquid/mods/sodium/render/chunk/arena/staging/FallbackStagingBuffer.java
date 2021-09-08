package me.jellysquid.mods.sodium.render.chunk.arena.staging;

import me.jellysquid.mods.thingl.buffer.GlBuffer;
import me.jellysquid.mods.thingl.buffer.GlBufferUsage;
import me.jellysquid.mods.thingl.buffer.GlMutableBuffer;
import me.jellysquid.mods.thingl.device.RenderDevice;

import java.nio.ByteBuffer;

public class FallbackStagingBuffer implements StagingBuffer {
    private final RenderDevice device;
    private final GlMutableBuffer fallbackBufferObject;

    public FallbackStagingBuffer(RenderDevice device) {
        this.fallbackBufferObject = device.createMutableBuffer();
        this.device = device;
    }

    @Override
    public void enqueueCopy(ByteBuffer data, GlBuffer dst, long writeOffset) {
        this.device.uploadData(this.fallbackBufferObject, data, GlBufferUsage.STREAM_COPY);
        this.device.copyBufferSubData(this.fallbackBufferObject, dst, 0, writeOffset, data.remaining());
    }

    @Override
    public void flush() {
        this.device.allocateStorage(this.fallbackBufferObject, 0L, GlBufferUsage.STREAM_COPY);
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
