package me.jellysquid.mods.sodium.render.stream;

import it.unimi.dsi.fastutil.objects.ObjectArrayFIFOQueue;
import me.jellysquid.mods.sodium.opengl.buffer.Buffer;
import me.jellysquid.mods.sodium.opengl.buffer.BufferMapFlags;
import me.jellysquid.mods.sodium.opengl.buffer.BufferStorageFlags;
import me.jellysquid.mods.sodium.opengl.buffer.MappedBuffer;
import me.jellysquid.mods.sodium.opengl.device.RenderDevice;
import me.jellysquid.mods.sodium.opengl.sync.Fence;
import me.jellysquid.mods.sodium.opengl.util.EnumBitField;
import me.jellysquid.mods.sodium.util.MathUtil;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.system.MemoryUtil;

import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;

public class MappedStreamingBuffer implements StreamingBuffer {
    private final RenderDevice device;
    private final MappedBuffer buffer;

    private final ObjectArrayFIFOQueue<Region> regions = new ObjectArrayFIFOQueue<>();

    private int position = 0;

    private final int capacity;
    private int remaining;

    private int mark;

    public MappedStreamingBuffer(RenderDevice device, int capacity) {
        this.device = device;
        this.buffer = device.createMappedBuffer(capacity,
                EnumBitField.of(BufferStorageFlags.PERSISTENT, BufferStorageFlags.MAP_WRITE, BufferStorageFlags.COHERENT),
                EnumBitField.of(BufferMapFlags.PERSISTENT, BufferMapFlags.WRITE, BufferMapFlags.COHERENT, BufferMapFlags.INVALIDATE_BUFFER, BufferMapFlags.UNSYNCHRONIZED));
        this.capacity = capacity;
        this.remaining = this.capacity;
        this.mark = 0;
    }

    @Override
    public Handle write(ByteBuffer data, int alignment) {
        int length = data.remaining();

        // We can't service allocations which are larger than the buffer itself
        if (length > this.capacity) {
            throw new OutOfMemoryError("data.remaining() > capacity");
        }

        int offset = MathHelper.roundUpToMultiple(this.position, alignment);

        if (offset + length > this.capacity) {
            this.flush0();

            this.position = 0;
            this.remaining = 0;

            offset = this.position;
        }

        // Reclaim memory regions until we have enough memory to service the request
        while (length > this.remaining) {
            this.reclaim();
        }

        this.buffer.write(offset, data);

        this.position = offset + length;
        this.remaining -= length;

        return new MappedHandle(offset, length);
    }

    @Override
    public Writer write(int length, int alignment) {
        // We can't service allocations which are larger than the buffer itself
        if (length > this.capacity) {
            throw new OutOfMemoryError("data.remaining() > capacity");
        }

        int offset = MathHelper.roundUpToMultiple(this.position, alignment);

        if (offset + length > this.capacity) {
            this.flush0();

            this.position = 0;
            this.remaining = 0;

            offset = this.position;
        }

        // Reclaim memory regions until we have enough memory to service the request
        while (length > this.remaining) {
            this.reclaim();
        }

        return new MappedWriter(this.buffer.getView(), offset, length);
    }

    private void flush0() {
        // Wrap the pointer around to zero if there's not enough space in the buffer
        int bytes = this.position - this.mark;

        if (bytes > 0) {
            this.regions.enqueue(new Region(this.mark, bytes, this.device.createFence()));
        }

        this.mark = this.position;
    }

    private void reclaim() {
        if (!this.regions.isEmpty()) {
            var region = this.regions.first();

            if (region.offset >= this.position) {
                region.fence.sync();

                this.regions.dequeue();
                this.remaining += region.length;

                return;
            }
        }

        this.remaining = this.capacity - this.position;
    }

    @Override
    public void flush() {
        this.flush0();
    }

    @Override
    public void delete() {
        while (!this.regions.isEmpty()) {
            var region = this.regions.dequeue();
            region.fence.sync();
        }

        this.device.deleteBuffer(this.buffer);
    }

    @Override
    public String getDebugString() {
        int used = 0;

        while (!this.regions.isEmpty()) {
            var region = this.regions.dequeue();
            used += region.length;
        }

        return String.format("%s/%s MiB", MathUtil.toMib(this.capacity - used), MathUtil.toMib(this.capacity));
    }

    private record Region(int offset, int length, Fence fence) {

    }

    private class MappedHandle implements Handle {
        private final int offset, length;

        private MappedHandle(int offset, int length) {
            this.offset = offset;
            this.length = length;
        }

        @Override
        public Buffer getBuffer() {
            return MappedStreamingBuffer.this.buffer;
        }

        @Override
        public int getOffset() {
            return this.offset;
        }

        @Override
        public int getLength() {
            return this.length;
        }

        @Override
        public void free() {

        }
    }

    private class MappedWriter implements Writer {
        private final int offset;
        private final long pointer;
        private final int capacity;

        private int length;

        public MappedWriter(ByteBuffer pointer, int offset, int bytes) {
            this.pointer = MemoryUtil.memAddress(pointer, offset);
            this.offset = offset;
            this.capacity = bytes;
        }

        @Override
        public long next(int bytes) {
            if (this.length > this.capacity) {
                throw new BufferOverflowException();
            }

            var pointer = this.pointer + this.length;
            this.length += bytes;

            return pointer;
        }

        @Override
        public Handle finish() {
            if (this.length == 0) {
                return null;
            }

            var handle = new MappedHandle(this.offset, this.length);

            MappedStreamingBuffer.this.position = this.offset + this.length;
            MappedStreamingBuffer.this.remaining -= this.length;

            return handle;
        }
    }
}
