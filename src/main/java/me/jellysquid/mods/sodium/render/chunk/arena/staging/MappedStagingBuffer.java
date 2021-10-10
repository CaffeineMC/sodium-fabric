package me.jellysquid.mods.sodium.render.chunk.arena.staging;

import it.unimi.dsi.fastutil.PriorityQueue;
import it.unimi.dsi.fastutil.objects.ObjectArrayFIFOQueue;
import me.jellysquid.mods.sodium.SodiumRender;
import me.jellysquid.mods.sodium.util.MathUtil;
import me.jellysquid.mods.thingl.buffer.*;
import me.jellysquid.mods.thingl.device.RenderDevice;
import me.jellysquid.mods.thingl.functions.BufferStorageFunctions;
import me.jellysquid.mods.thingl.sync.Fence;
import me.jellysquid.mods.thingl.util.EnumBitField;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

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

            ByteBuffer firstSectionSrc, secondSectionSrc;
            if (SodiumRender.isDirectMemoryAccessEnabled()) {
                firstSectionSrc = MemoryUtil.memSlice(data, 0, remaining);
                secondSectionSrc = MemoryUtil.memSlice(data, remaining, split);
            } else {
                firstSectionSrc = data.slice(0, remaining);
                secondSectionSrc = data.slice(remaining, split);
            }

            this.addTransfer(firstSectionSrc, dst, this.pos, writeOffset);
            this.addTransfer(secondSectionSrc, dst, 0, writeOffset + remaining);

            this.pos = split;
        } else {
            this.addTransfer(data, dst, this.pos, writeOffset);
            this.pos += length;
        }

        this.remaining -= length;
    }

    private void addTransfer(ByteBuffer data, Buffer dst, long readOffset, long writeOffset) {
        ByteBuffer mappedBufferPointer = this.mappedBuffer.map.getPointer();
        if (SodiumRender.isDirectMemoryAccessEnabled()) {
            MemoryUtil.memCopy(MemoryUtil.memAddress(data), MemoryUtil.memAddress(mappedBufferPointer, (int) readOffset), data.remaining()); // is this meant to use readOffset?
        } else {
            mappedBufferPointer.put((int) readOffset, data, data.position(), data.remaining());
        }
        this.pendingCopies.enqueue(new CopyCommand(dst, readOffset, writeOffset, data.remaining()));
    }

    @Override
    public void flush() {
        if (this.pendingCopies.isEmpty()) {
            return;
        }

        if (this.pos < this.start) {
            this.device.flushMappedRange(this.mappedBuffer.map, this.start, this.capacity - this.start);
            this.device.flushMappedRange(this.mappedBuffer.map, 0, this.pos);
        } else {
            this.device.flushMappedRange(this.mappedBuffer.map, this.start, this.pos - this.start);
        }

        int bytes = 0;

        for (CopyCommand command : consolidateCopies(this.pendingCopies)) {
            bytes += command.bytes;

            this.device.copyBufferSubData(this.mappedBuffer.buffer, command.buffer, command.readOffset, command.writeOffset, command.bytes);
        }

        this.fencedRegions.enqueue(new FencedMemoryRegion(this.device.createFence(), bytes));

        this.start = this.pos;
    }

    private static List<CopyCommand> consolidateCopies(PriorityQueue<CopyCommand> queue) {
        List<CopyCommand> merged = new ArrayList<>();
        CopyCommand last = null;

        while (!queue.isEmpty()) {
            CopyCommand command = queue.dequeue();

            if (last != null) {
                if (last.buffer == command.buffer &&
                        last.writeOffset + last.bytes == command.writeOffset &&
                        last.readOffset + last.bytes == command.readOffset) {
                    last.bytes += command.bytes;
                    continue;
                }
            }

            merged.add(last = new CopyCommand(command));
        }

        return merged;
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

    private static final class CopyCommand {
        private final Buffer buffer;
        private final long readOffset;
        private final long writeOffset;

        private long bytes;

        private CopyCommand(Buffer buffer, long readOffset, long writeOffset, long bytes) {
            this.buffer = buffer;
            this.readOffset = readOffset;
            this.writeOffset = writeOffset;
            this.bytes = bytes;
        }

        public CopyCommand(CopyCommand command) {
            this.buffer = command.buffer;
            this.writeOffset = command.writeOffset;
            this.readOffset = command.readOffset;
            this.bytes = command.bytes;
        }
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
