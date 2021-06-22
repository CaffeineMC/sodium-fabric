package me.jellysquid.mods.sodium.client.gl.arena;

import it.unimi.dsi.fastutil.objects.ObjectLinkedOpenHashSet;
import me.jellysquid.mods.sodium.client.gl.buffer.GlBuffer;
import me.jellysquid.mods.sodium.client.gl.buffer.GlBufferTarget;
import me.jellysquid.mods.sodium.client.gl.buffer.GlBufferUsage;
import me.jellysquid.mods.sodium.client.gl.buffer.GlMutableBuffer;
import me.jellysquid.mods.sodium.client.gl.device.CommandList;

import java.nio.ByteBuffer;
import java.util.Set;

public class GlBufferArena {
    private static final GlBufferUsage BUFFER_USAGE = GlBufferUsage.GL_DYNAMIC_DRAW;

    private final int stride;
    private final int resizeIncrement;

    private final Set<GlBufferSegment> freeRegions = new ObjectLinkedOpenHashSet<>();

    private final GlMutableBuffer stagingBuffer;
    private GlMutableBuffer arenaBuffer;

    private int head;
    private int capacity;
    private int allocCount;

    public GlBufferArena(CommandList commands, int initialCapacity, int stride) {
        commands.allocateBuffer(GlBufferTarget.COPY_WRITE_BUFFER,
                this.arenaBuffer = commands.createMutableBuffer(BUFFER_USAGE), (long) initialCapacity * stride);

        this.stride = stride;
        this.stagingBuffer = commands.createMutableBuffer(GlBufferUsage.GL_STATIC_DRAW);

        this.resizeIncrement = initialCapacity;
        this.capacity = initialCapacity;
    }

    private void resize(CommandList commandList, int capacity) {
        if (this.capacity >= capacity) {
            throw new UnsupportedOperationException("New capacity must be larger than previous");
        }

        GlMutableBuffer src = this.arenaBuffer;
        GlMutableBuffer dst = commandList.createMutableBuffer(BUFFER_USAGE);

        commandList.allocateBuffer(GlBufferTarget.COPY_WRITE_BUFFER, dst, (long) capacity * this.stride);
        commandList.copyBufferSubData(src, dst, 0, 0, (long) this.head * this.stride);
        commandList.deleteBuffer(src);

        this.arenaBuffer = dst;
        this.capacity = capacity;
    }

    public void checkArenaCapacity(CommandList commandList, int count) {
        if (this.head + count >= this.capacity) {
            this.resize(commandList, this.getNextSize(count));
        }
    }

    public GlBufferSegment uploadBuffer(CommandList commandList, ByteBuffer buffer) {
        int elementCount = buffer.remaining() / this.stride;

        this.checkArenaCapacity(commandList, elementCount);

        GlBufferSegment segment = this.alloc(elementCount);

        commandList.uploadData(this.stagingBuffer, buffer);
        commandList.copyBufferSubData(this.stagingBuffer, this.arenaBuffer,
                0, (long) segment.getElementOffset() * this.stride, (long) segment.getElementCount() * this.stride);
        commandList.invalidateBuffer(this.stagingBuffer);

        return segment;
    }

    private int getNextSize(int count) {
        return Math.max(this.capacity + this.resizeIncrement, this.capacity + count);
    }

    public void free(GlBufferSegment segment) {
        if (!this.freeRegions.add(segment)) {
            throw new IllegalArgumentException("Segment already freed");
        }

        this.allocCount--;
    }

    private GlBufferSegment alloc(int len) {
        GlBufferSegment segment = this.allocReuse(len);

        if (segment == null) {
            segment = new GlBufferSegment(this, this.head, len);

            this.head += len;
        }

        this.allocCount++;

        return segment;
    }

    private GlBufferSegment allocReuse(int len) {
        GlBufferSegment bestSegment = null;

        for (GlBufferSegment segment : this.freeRegions) {
            if (segment.getElementCount() < len) {
                continue;
            }

            if (bestSegment == null || bestSegment.getElementCount() > segment.getElementCount()) {
                bestSegment = segment;
            }
        }

        if (bestSegment == null) {
            return null;
        }

        this.freeRegions.remove(bestSegment);

        int excess = bestSegment.getElementCount() - len;

        if (excess > 0) {
            this.freeRegions.add(new GlBufferSegment(this, bestSegment.getElementOffset() + len, excess));
        }

        return new GlBufferSegment(this, bestSegment.getElementOffset(), len);
    }

    public void delete(CommandList commands) {
        commands.deleteBuffer(this.arenaBuffer);
        commands.deleteBuffer(this.stagingBuffer);
    }

    public boolean isEmpty() {
        return this.allocCount <= 0;
    }

    public GlBuffer getBufferObject() {
        return this.arenaBuffer;
    }
}
