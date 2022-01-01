package me.jellysquid.mods.sodium.client.render.immediate.stream;

import me.jellysquid.mods.sodium.client.gl.buffer.*;
import me.jellysquid.mods.sodium.client.gl.device.CommandList;
import me.jellysquid.mods.sodium.client.gl.device.RenderDevice;
import me.jellysquid.mods.sodium.client.gl.sync.GlFence;
import me.jellysquid.mods.sodium.client.gl.util.EnumBitField;
import net.minecraft.util.math.MathHelper;

import java.nio.ByteBuffer;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.function.Supplier;

public class MappedStreamingBuffer implements StreamingBuffer {
    private final GlImmutableBuffer buffer;
    private final GlBufferMapping mapping;

    private final Deque<Handle> lockedRegions = new ArrayDeque<>();

    private int pos = 0;

    private final int capacity;
    private int remaining;

    public MappedStreamingBuffer(CommandList commandList, int capacity) {
        this.buffer = commandList.createImmutableBuffer(capacity, EnumBitField.of(GlBufferStorageFlags.PERSISTENT, GlBufferStorageFlags.CLIENT_STORAGE, GlBufferStorageFlags.COHERENT, GlBufferStorageFlags.MAP_WRITE));
        this.mapping = commandList.mapBuffer(this.buffer, 0, capacity, EnumBitField.of(GlBufferMapFlags.PERSISTENT, GlBufferMapFlags.WRITE, GlBufferMapFlags.COHERENT, GlBufferMapFlags.INVALIDATE_BUFFER, GlBufferMapFlags.UNSYNCHRONIZED));
        this.capacity = capacity;
        this.remaining = this.capacity;
    }

    public static boolean isSupported(RenderDevice device) {
        return device.getDeviceFunctions().getBufferStorageFunctions() != null;
    }

    @Override
    public BufferHandle write(CommandList commandList, ByteBuffer data, int alignment) {
        var last = this.lockedRegions.peekLast();

        if (last != null && last.fence == null) {
            throw new IllegalStateException("Previous handle was not finished");
        }

        int length = data.remaining();

        if (length > this.capacity) {
            throw new OutOfMemoryError();
        }

        int pos = MathHelper.roundUpToMultiple(this.pos, alignment);

        if (pos + length > this.capacity) {
            this.pos = 0;
            this.remaining = 0;

            pos = 0;
        }

        while (length > this.remaining) {
            if (this.lockedRegions.isEmpty()) {
                this.remaining = this.capacity - this.pos;
                break;
            }

            var handle = this.lockedRegions.remove();
            handle.fence.sync();

            this.remaining += handle.getLength();
        }

        this.mapping.write(data, pos);
        this.pos = pos + length;

        this.remaining -= length;

        var region = new Handle(pos, alignment, length);
        this.lockedRegions.add(region);

        return region;
    }


    @Override
    public GlBuffer getBuffer() {
        return this.buffer;
    }

    @Override
    public void delete(CommandList commandList) {
        commandList.unmap(this.mapping);

        while (!this.lockedRegions.isEmpty()) {
            var region = this.lockedRegions.remove();
            region.fence.sync();
        }

        commandList.deleteBuffer(this.buffer);
    }

    private static class Handle extends AbstractBufferHandle {
        private GlFence fence;

        public Handle(int offset, int stride, int length) {
            super(offset, stride, length);
        }

        @Override
        public void finish(Supplier<GlFence> fence) {
            if (this.fence != null) {
                throw new IllegalStateException("Handle has already been finished");
            }

            this.fence = fence.get();
        }
    }
}
