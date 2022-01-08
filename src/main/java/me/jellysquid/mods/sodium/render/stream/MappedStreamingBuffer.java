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
import java.util.Iterator;

// WARNING: Not thread safe. Buffer ranges are NOT LOCKED on the CPU.
public class MappedStreamingBuffer implements StreamingBuffer {
    private final RenderDevice device;
    private final FlushableMappedBuffer buffer;

    private final Deque<Region> regions = new ArrayDeque<>();

    public MappedStreamingBuffer(RenderDevice device, long capacity) {
        this.device = device;
        this.buffer = device.createFlushableMappedBuffer(capacity,
                EnumBitField.of(BufferStorageFlags.PERSISTENT, BufferStorageFlags.MAP_WRITE),
                EnumBitField.of(BufferMapFlags.PERSISTENT, BufferMapFlags.WRITE, BufferMapFlags.INVALIDATE_BUFFER, BufferMapFlags.UNSYNCHRONIZED));
    }

    @Override
    public ByteBuffer allocate(long length, long alignment) {
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
        // TODO: use some sort of interval tree for faster lookups
        Iterator<Region> regionIterator = regions.iterator();
        while (regionIterator.hasNext()) {
            Region region = regionIterator.next();
            if (pos <= region.offset && pos + length <= region.offset + region.length) {
                region.fence.sync();
                regionIterator.remove();
            }
        }

        // ByteBuffer doesn't support longs
        ByteBuffer newRegionPointer = MemoryUtil.memDuplicate(bufferPointer);
        newRegionPointer.position((int) pos);
        newRegionPointer.limit((int) (pos + length));
        bufferPointer.position((int) (pos + length));

        return newRegionPointer;
    }

    @Override
    public void flushRegion(ByteBuffer region) {
        this.buffer.flush(region.position(), region.limit() - region.position());
    }

    @Override
    public void fenceRegion(ByteBuffer region) {
        Fence fence = this.device.createFence();
        this.regions.add(new Region(region.position(), region.limit() - region.position(), fence));
    }

    @Override
    public Buffer getBuffer() {
        return this.buffer;
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

        long capacity = this.buffer.getCapacity();
        return String.format("%s/%s MiB", MathUtil.toMib(capacity - used), MathUtil.toMib(capacity));
    }

    private record Region(int offset, int length, Fence fence) {

    }

}
