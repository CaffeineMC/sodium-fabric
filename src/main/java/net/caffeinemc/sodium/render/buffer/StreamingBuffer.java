package net.caffeinemc.sodium.render.buffer;

import net.caffeinemc.gfx.api.buffer.Buffer;
import net.caffeinemc.gfx.api.buffer.MappedBuffer;
import net.caffeinemc.gfx.api.buffer.MappedBufferFlags;
import net.caffeinemc.gfx.api.device.RenderDevice;
import net.caffeinemc.sodium.util.MathUtil;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.util.EnumSet;

public class StreamingBuffer {
    private final RenderDevice device;
    private final MappedBuffer buffer;
    private final int frameCount;
    private final int stride;
    private final int alignedStride;

    public StreamingBuffer(RenderDevice device, int stride, int frameCount) {
        var alignment = device.properties().uniformBufferOffsetAlignment;

        this.stride = stride;
        this.alignedStride = MathUtil.align(stride, alignment);
        this.buffer = device.createMappedBuffer(this.alignedStride * frameCount, EnumSet.of(MappedBufferFlags.WRITE));
        this.frameCount = frameCount;
        this.device = device;
    }

    public Slice slice(int frameIndex) {
        int start = this.alignedStride * (frameIndex % this.frameCount);
        var view = MemoryUtil.memSlice(this.buffer.view(), start, this.stride);

        return new Slice(this.buffer, view, start, this.stride);
    }

    public void delete() {
        this.device.deleteBuffer(this.buffer);
    }

    public record Slice(Buffer buffer, ByteBuffer view, long offset, long length) {

    }
}
