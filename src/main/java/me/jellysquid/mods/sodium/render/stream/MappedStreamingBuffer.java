package me.jellysquid.mods.sodium.render.stream;

import me.jellysquid.mods.sodium.opengl.buffer.Buffer;
import me.jellysquid.mods.sodium.opengl.buffer.BufferMapFlags;
import me.jellysquid.mods.sodium.opengl.buffer.BufferStorageFlags;
import me.jellysquid.mods.sodium.opengl.buffer.FlushableMappedBuffer;
import me.jellysquid.mods.sodium.opengl.device.RenderDevice;
import me.jellysquid.mods.sodium.opengl.sync.Fence;
import me.jellysquid.mods.sodium.opengl.util.EnumBitField;
import me.jellysquid.mods.sodium.util.MathUtil;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.util.ArrayDeque;
import java.util.Deque;

public class MappedStreamingBuffer implements StreamingBuffer {
    private final RenderDevice device;
    private final FlushableMappedBuffer buffer;

    private final Deque<StreamingBufferRegion> regions = new ArrayDeque<>(); // not thread safe

    public MappedStreamingBuffer(RenderDevice device, long capacity) {
        this.device = device;
        this.buffer = device.createFlushableMappedBuffer(capacity,
                EnumBitField.of(BufferStorageFlags.PERSISTENT, BufferStorageFlags.MAP_WRITE),
                EnumBitField.of(BufferMapFlags.PERSISTENT, BufferMapFlags.WRITE, BufferMapFlags.INVALIDATE_BUFFER, BufferMapFlags.UNSYNCHRONIZED));
    }

    @Override
    public StreamingBufferRegion allocate(long length, long alignment) {
        // We can't service allocations which are larger than the buffer itself
        long capacity = this.buffer.getCapacity();
        if (length > capacity) {
            throw new OutOfMemoryError("data.remaining() > capacity");
        }

        ByteBuffer bufferPointer = this.buffer.getPointer();

        // Align the pointer so that we can return element offsets from this buffer
        int initialPos = bufferPointer.position();
        long pos = MathUtil.roundUpToMultiple(initialPos, alignment);

        // Wrap the pointer around to zero if there's not enough space in the buffer
        if (pos + length > capacity) {
            pos = 0;
        }

        // Sync all intersecting regions and get rid of them
        // TODO: create thread safe impl of this
        StreamingBufferRegion firstRegion = regions.peekFirst();
        while (firstRegion != null && pos <= firstRegion.getOffset() + firstRegion.getLength() && pos + length >= firstRegion.getOffset()) {
            Fence fence = firstRegion.getFence();
            if (fence == null) throw new IllegalStateException("Fence null, not thread safe yet");
            fence.sync();
            regions.pollFirst();
            firstRegion = regions.peekFirst();
        }

        // ByteBuffer doesn't support longs
        ByteBuffer newRegionPointer = MemoryUtil.memByteBuffer(MemoryUtil.memAddress0(bufferPointer) + pos, (int) length);
        bufferPointer.position((int) (pos + length));

        StreamingBufferRegion newRegion = new StreamingBufferRegion(pos, length, newRegionPointer);
        this.regions.add(newRegion);

        return newRegion;
    }

    @Override
    public void flushRegion(StreamingBufferRegion region) {
        this.buffer.flush(region.getOffset(), region.getLength());
    }

    @Override
    public void fenceRegion(StreamingBufferRegion region) {
        region.setFence(this.device.createFence());
    }

    @Override
    public Buffer getBuffer() {
        return this.buffer;
    }

    @Override
    public void delete() {
        while (!this.regions.isEmpty()) {
            var region = this.regions.remove();
            region.getFence().sync();
        }
        this.device.deleteBuffer(this.buffer);
    }

    @Override
    public String getDebugString() {
        var it = this.regions.iterator();
        int used = 0;
        while (it.hasNext()) {
            var region = it.next();
            used += region.getLength();
        }

        long capacity = this.buffer.getCapacity();
        return String.format("%s/%s MiB", MathUtil.toMib(capacity - used), MathUtil.toMib(capacity));
    }

}
