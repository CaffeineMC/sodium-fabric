package net.caffeinemc.sodium.render.buffer;

import net.caffeinemc.gfx.api.buffer.MappedBuffer;
import net.caffeinemc.gfx.api.buffer.MappedBufferFlags;
import net.caffeinemc.gfx.api.device.RenderDevice;
import net.caffeinemc.gfx.api.sync.Fence;
import net.caffeinemc.sodium.util.MathUtil;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.util.EnumSet;

// FIXME: synchronization? barriers? where are they?
public class StreamingBuffer {
    private final RenderDevice device;
    private final MappedBuffer buffer;
    private final Fence[] sliceFences;
    private final int frameCount;
    private final int stride;
    private final int alignedStride;

    public StreamingBuffer(RenderDevice device, boolean explicitFlush, int alignment, int stride, int frameCount) {
        this.device = device;
        this.stride = stride;
        this.frameCount = frameCount;
        this.alignedStride = MathUtil.align(stride, alignment);
        this.sliceFences = new Fence[frameCount];

        var flags = EnumSet.of(MappedBufferFlags.WRITE);
        if (explicitFlush) {
            flags.add(MappedBufferFlags.EXPLICIT_FLUSH);
        }
        this.buffer = device.createMappedBuffer((long) this.alignedStride * frameCount, flags);
    }

    public Slice slice(int frameIndex) {
        int sliceIdx = frameIndex % this.frameCount;

        Fence fence = this.sliceFences[sliceIdx];
        if (fence != null) {
            fence.sync();
        }
        this.sliceFences[sliceIdx] = this.device.createFence();

        int start = this.alignedStride * sliceIdx;
        var view = MemoryUtil.memSlice(this.buffer.view(), start, this.stride);

        return new Slice(this.buffer, view, start, this.stride);
    }

    public void delete() {
        this.device.deleteBuffer(this.buffer);
        for (Fence fence : this.sliceFences) {
            if (fence != null) {
                fence.delete();
            }
        }
    }

    public record Slice(MappedBuffer buffer, ByteBuffer view, long offset, long length) {
        public void flush() {
            this.buffer.flush(this.offset, this.length);
        }
    }
}
