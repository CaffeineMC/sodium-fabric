package me.jellysquid.mods.sodium.render.chunk.arena.staging;

import it.unimi.dsi.fastutil.PriorityQueue;
import it.unimi.dsi.fastutil.objects.ObjectArrayFIFOQueue;
import me.jellysquid.mods.sodium.util.MathUtil;
import me.jellysquid.mods.thingl.buffer.*;
import me.jellysquid.mods.thingl.device.RenderDevice;
import me.jellysquid.mods.thingl.functions.BufferStorageFunctions;
import me.jellysquid.mods.thingl.sync.Fence;
import me.jellysquid.mods.thingl.util.EnumBitField;

import java.nio.ByteBuffer;

public class MappedStagingBuffer implements StagingBuffer {
    private static final EnumBitField<BufferStorageFlags> STORAGE_FLAGS =
            EnumBitField.of(BufferStorageFlags.PERSISTENT, BufferStorageFlags.CLIENT_STORAGE, BufferStorageFlags.MAP_WRITE);

    private static final EnumBitField<BufferMapFlags> MAP_FLAGS =
            EnumBitField.of(BufferMapFlags.PERSISTENT, BufferMapFlags.INVALIDATE_BUFFER, BufferMapFlags.WRITE, BufferMapFlags.EXPLICIT_FLUSH);

    private final FallbackStagingBuffer fallbackStagingBuffer;

    private final RenderDevice device;
    private final MappedBuffer mappedBuffer;
    private final PriorityQueue<CopyCommand> pendingCopies = new ObjectArrayFIFOQueue<>();
    private final PriorityQueue<FencedMemoryRegion> fencedRegions = new ObjectArrayFIFOQueue<>();

    private int start = 0;
    private int pos = 0;

    private final int capacity;
    private int remaining;

    public MappedStagingBuffer(RenderDevice device) {
        this(device, 1024 * 1024 * 16 /* 16 MB */);
    }

    public MappedStagingBuffer(RenderDevice device, int capacity) {
        ImmutableBuffer buffer = device.createImmutableBuffer(capacity, STORAGE_FLAGS);
        BufferMapping map = device.mapBuffer(buffer, 0, capacity, MAP_FLAGS);

        this.device = device;
        this.mappedBuffer = new MappedBuffer(buffer, map);
        this.fallbackStagingBuffer = new FallbackStagingBuffer(device);
        this.capacity = capacity;
        this.remaining = this.capacity;
    }

    public static boolean isSupported(RenderDevice instance) {
        return instance.getDeviceFunctions().getBufferStorageFunctions() != BufferStorageFunctions.NONE;
    }

    @Override
    public void enqueueCopy(ByteBuffer data, Buffer dst, long writeOffset) {
        int length = data.remaining();

        if (length > this.remaining) {
            this.fallbackStagingBuffer.enqueueCopy(data, dst, writeOffset);

            return;
        }

        int remaining = this.capacity - this.pos;

        // Split the transfer in two if we have enough available memory at the end and start of the buffer
        if (length > remaining) {
            int split = length - remaining;

            this.addTransfer(data.slice(0, remaining), dst, this.pos, writeOffset);
            this.addTransfer(data.slice(remaining, split), dst, 0, writeOffset + remaining);

            this.pos = split;
        } else {
            this.addTransfer(data, dst, this.pos, writeOffset);
            this.pos += length;
        }

        this.remaining -= length;
    }

    private void addTransfer(ByteBuffer data, Buffer dst, long readOffset, long writeOffset) {
        this.mappedBuffer.map.write(data, (int) readOffset);
        this.pendingCopies.enqueue(new CopyCommand(dst, readOffset, writeOffset, data.remaining()));
    }

    @Override
    public void flush() {
        if (this.pendingCopies.isEmpty()) {
            return;
        }

        int bytes = 0;

        if (this.pos < this.start) {
            this.device.flushMappedRange(this.mappedBuffer.map, this.start, this.capacity - this.start);
            this.device.flushMappedRange(this.mappedBuffer.map, 0, this.pos);
        } else {
            this.device.flushMappedRange(this.mappedBuffer.map, this.start, this.pos - this.start);
        }

        while (!this.pendingCopies.isEmpty()) {
            CopyCommand command = this.pendingCopies.dequeue();
            this.device.copyBufferSubData(this.mappedBuffer.buffer, command.buffer, command.readOffset, command.writeOffset, command.bytes);

            bytes += command.bytes;
        }

        this.fencedRegions.enqueue(new FencedMemoryRegion(this.device.createFence(), bytes));
        this.start = this.pos;
    }

    @Override
    public void delete() {
        this.mappedBuffer.delete();
        this.fallbackStagingBuffer.delete();
        this.pendingCopies.clear();
    }

    @Override
    public void flip() {
        while (!this.fencedRegions.isEmpty()) {
            FencedMemoryRegion region = this.fencedRegions.first();
            Fence fence = region.fence();

            if (!fence.isCompleted()) {
                break;
            }

            this.device.deleteFence(fence);

            this.fencedRegions.dequeue();
            this.remaining += region.length();
        }
    }

    private record CopyCommand(Buffer buffer, long readOffset, long writeOffset, long bytes) {
    }

    private class MappedBuffer {
        private final ImmutableBuffer buffer;
        private final BufferMapping map;

        private MappedBuffer(ImmutableBuffer buffer,
                             BufferMapping map) {
            this.buffer = buffer;
            this.map = map;
        }

        public void delete() {
            MappedStagingBuffer.this.device.unmap(this.map);
            MappedStagingBuffer.this.device.deleteBuffer(this.buffer);
        }
    }

    private record FencedMemoryRegion(Fence fence, int length) {

    }

    @Override
    public String toString() {
        return "Mapped (%s/%s MiB)".formatted(MathUtil.toMib(this.remaining), MathUtil.toMib(this.capacity));
    }
}
