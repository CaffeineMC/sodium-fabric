package net.caffeinemc.sodium.render.buffer;

import net.caffeinemc.gfx.api.buffer.Buffer;
import net.caffeinemc.gfx.api.buffer.BufferMapFlags;
import net.caffeinemc.gfx.api.buffer.BufferStorageFlags;
import net.caffeinemc.gfx.api.buffer.MappedBuffer;
import net.caffeinemc.gfx.api.device.RenderDevice;
import net.caffeinemc.sodium.util.MathUtil;
import org.apache.commons.lang3.Validate;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.util.Set;

public class StreamingBuffer {
    private final RenderDevice device;
    private final MappedBuffer buffer;
    private final int frameCount;
    private final int stride;
    private final int alignedStride;

    public StreamingBuffer(RenderDevice device, Set<BufferStorageFlags> storageFlags, Set<BufferMapFlags> mapFlags,
                           int stride, int frameCount) {
        var alignment = device.properties().uniformBufferOffsetAlignment;

        this.stride = stride;
        this.alignedStride = MathUtil.align(stride, alignment);
        this.buffer = device.createMappedBuffer(this.alignedStride * frameCount, storageFlags, mapFlags);
        this.frameCount = frameCount;
        this.device = device;
    }

    public Slice slice(int frameIndex) {
        int start = this.alignedStride * (frameIndex % this.frameCount);
        var view = MemoryUtil.memSlice(this.buffer.view(), start, this.stride);

        return new Slice(this.buffer, view, start, this.stride);
    }

    public void flush(Slice slice) {
        Validate.isTrue(this.buffer == slice.buffer(), "Slice does not belong to this object");

        this.buffer.flush(slice.offset(), slice.length());
    }

    public void delete() {
        this.device.deleteBuffer(this.buffer);
    }

    public record Slice(Buffer buffer, ByteBuffer view, long offset, long length) {

    }
}
