package me.jellysquid.mods.sodium.render.stream;

import me.jellysquid.mods.sodium.opengl.device.CommandList;
import me.jellysquid.mods.sodium.opengl.sync.GlFence;
import me.jellysquid.mods.sodium.opengl.util.EnumBitField;
import me.jellysquid.mods.sodium.opengl.buffer.GlBuffer;
import me.jellysquid.mods.sodium.opengl.buffer.GlBufferMapFlags;
import me.jellysquid.mods.sodium.opengl.buffer.GlBufferStorageFlags;
import me.jellysquid.mods.sodium.opengl.buffer.GlMappedBuffer;
import net.minecraft.util.math.MathHelper;

import java.nio.ByteBuffer;
import java.util.ArrayDeque;
import java.util.Deque;

public class MappedStreamingBuffer implements StreamingBuffer {
    private final GlMappedBuffer buffer;

    private final Deque<Region> regions = new ArrayDeque<>();

    private int pos = 0;

    private final int capacity;
    private int remaining;

    private int mark;
    private int queued;

    public MappedStreamingBuffer(CommandList commandList, int capacity) {
        this.buffer = commandList.createMappedBuffer(capacity,
                EnumBitField.of(GlBufferStorageFlags.PERSISTENT, GlBufferStorageFlags.CLIENT_STORAGE, GlBufferStorageFlags.COHERENT, GlBufferStorageFlags.MAP_WRITE),
                EnumBitField.of(GlBufferMapFlags.PERSISTENT, GlBufferMapFlags.WRITE, GlBufferMapFlags.COHERENT, GlBufferMapFlags.INVALIDATE_BUFFER, GlBufferMapFlags.UNSYNCHRONIZED));
        this.capacity = capacity;
        this.remaining = this.capacity;
        this.mark = 0;
    }

    @Override
    public int write(CommandList commandList, ByteBuffer data, int alignment) {
        int length = data.remaining();

        // We can't service allocations which are larger than the buffer itself
        if (length > this.capacity) {
            throw new OutOfMemoryError("data.remaining() > capacity");
        }

        // Align the pointer so that we can return element offsets from this buffer
        int pos = MathHelper.roundUpToMultiple(this.pos, alignment);

        // Wrap the pointer around to zero if there's not enough space in the buffer
        if (pos + length > this.capacity) {
            pos = this.wrap(commandList, alignment);
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

    private int wrap(CommandList commandList, int alignment) {
        // When we wrap around, we need to ensure any memory behind us has a fence created
        if (this.queued > 0) {
            this.insertFence(commandList, this.mark, this.queued);
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
    public GlBuffer getBuffer() {
        return this.buffer;
    }

    @Override
    public void flush(CommandList commandList) {
        // No data to flush
        if (this.queued <= 0) {
            return;
        }

        // If we wrapped around since the last flush, then fence both the head and tail of the buffer
        if (this.mark + this.queued > this.capacity) {
            this.insertFence(commandList, this.mark, this.capacity - this.mark);
            this.insertFence(commandList, 0, this.mark);
        } else {
            this.insertFence(commandList, this.mark, this.queued);
        }

        this.queued = 0;
        this.mark = this.pos;
    }

    private void insertFence(CommandList commandList, int offset, int length) {
        var fence = commandList.createFence();
        var region = new Region(offset, length, fence);

        this.regions.add(region);
    }

    @Override
    public void delete(CommandList commandList) {
        while (!this.regions.isEmpty()) {
            var region = this.regions.remove();
            region.fence.sync();
        }

        commandList.deleteBuffer(this.buffer);
    }

    private record Region(int offset, int length, GlFence fence) {

    }
}
