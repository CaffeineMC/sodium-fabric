package me.jellysquid.mods.sodium.render.stream;

import me.jellysquid.mods.sodium.opengl.buffer.*;
import me.jellysquid.mods.sodium.opengl.device.RenderDevice;
import me.jellysquid.mods.sodium.opengl.sync.Fence;
import me.jellysquid.mods.sodium.opengl.util.EnumBitField;
import me.jellysquid.mods.sodium.util.MathUtil;
import net.minecraft.util.math.MathHelper;

import java.nio.ByteBuffer;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;

public class MappedStreamingBuffer implements StreamingBuffer {
    private final RenderDevice device;
    private final MappedBuffer buffer;

    private final Deque<Region> regions = new ArrayDeque<>();

    private int pos = 0;

    private final int capacity;
    private int remaining;

    private int mark;
    private int queued;

    public MappedStreamingBuffer(RenderDevice device, int capacity) {
        this.device = device;
        this.buffer = device.createMappedBuffer(capacity,
                EnumBitField.of(BufferStorageFlags.PERSISTENT, BufferStorageFlags.COHERENT, BufferStorageFlags.MAP_WRITE),
                EnumBitField.of(BufferMapFlags.PERSISTENT, BufferMapFlags.WRITE, BufferMapFlags.COHERENT, BufferMapFlags.INVALIDATE_BUFFER, BufferMapFlags.UNSYNCHRONIZED));
        this.capacity = capacity;
        this.remaining = this.capacity;
        this.mark = 0;
    }

    @Override
    public int write(ByteBuffer data, int alignment) {
        int length = data.remaining();

        // We can't service allocations which are larger than the buffer itself
        if (length > this.capacity) {
            throw new OutOfMemoryError("data.remaining() > capacity");
        }

        // Align the pointer so that we can return element offsets from this buffer
        int pos = MathHelper.roundUpToMultiple(this.pos, alignment);

        // Wrap the pointer around to zero if there's not enough space in the buffer
        if (pos + length > this.capacity) {
            pos = this.wrap(alignment);
        }

        // Reclaim memory regions until we have enough memory to service the request
        while (length > this.remaining) {
            this.reclaim();
        }

        this.buffer.write(data, pos);
        this.pos = pos + length;

        this.remaining -= length;
        this.queued += length;

        return pos / alignment;
    }

    private int wrap(int alignment) {
        // When we wrap around, we need to ensure any memory behind us has a fence created
        if (this.queued > 0) {
            this.insertFence(this.mark, this.queued);
        }

        this.mark = 0;
        this.queued = 0;
        this.pos = 0;
        this.remaining = 0;

        // Position is always zero when wrapping around, so the alignment doesn't matter
        return 0;
    }

    private void reclaim() {
        if (this.regions.isEmpty()) {
            this.remaining = this.capacity - this.pos;
        } else {
            var handle = this.regions.remove();
            handle.fence.sync();

            this.remaining += handle.length;
        }
    }

    @Override
    public Buffer getBuffer() {
        return this.buffer;
    }

    @Override
    public void flush() {
        // Poll fence objects and release regions
        while (!this.regions.isEmpty()) {
            var region = this.regions.peek();

            if (!region.fence.poll()) {
                break;
            }

            this.regions.remove();
        }

        // No data to flush
        if (this.queued <= 0) {
            return;
        }

        // If we wrapped around since the last flush, then fence both the head and tail of the buffer
        if (this.mark + this.queued > this.capacity) {
            this.insertFence(this.mark, this.capacity - this.mark);
            this.insertFence(0, this.mark);
        } else {
            this.insertFence(this.mark, this.queued);
        }

        this.queued = 0;
        this.mark = this.pos;
    }

    private void insertFence(int offset, int length) {
        var fence = this.device.createFence();
        var region = new Region(offset, length, fence);

        this.regions.add(region);
    }

    @Override
    public void delete() {
        while (!this.regions.isEmpty()) {
            var region = this.regions.remove();
            region.fence.sync();
        }

        this.device.deleteBuffer(this.buffer);
    }

    @Override
    public String getDebugString() {
        var it = this.regions.iterator();

        int used = 0;

        while (it.hasNext()) {
            var region = it.next();
            used += region.length;
        }

        return String.format("%s/%s MiB", MathUtil.toMib(this.capacity - used), MathUtil.toMib(this.capacity));
    }

    private record Region(int offset, int length, Fence fence) {

    }
}
