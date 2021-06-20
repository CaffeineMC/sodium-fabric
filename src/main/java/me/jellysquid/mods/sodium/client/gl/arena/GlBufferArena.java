package me.jellysquid.mods.sodium.client.gl.arena;

import it.unimi.dsi.fastutil.objects.ObjectLinkedOpenHashSet;
import me.jellysquid.mods.sodium.client.gl.buffer.GlBuffer;
import me.jellysquid.mods.sodium.client.gl.buffer.GlBufferTarget;
import me.jellysquid.mods.sodium.client.gl.buffer.GlBufferUsage;
import me.jellysquid.mods.sodium.client.gl.buffer.GlMutableBuffer;
import me.jellysquid.mods.sodium.client.gl.device.CommandList;
import me.jellysquid.mods.sodium.client.gl.device.RenderDevice;

import java.nio.ByteBuffer;
import java.util.Set;

public class GlBufferArena {
    private static final GlBufferUsage BUFFER_USAGE = GlBufferUsage.GL_DYNAMIC_DRAW;

    private final RenderDevice device;
    private final int resizeIncrement;

    private final Set<GlBufferSegment> freeRegions = new ObjectLinkedOpenHashSet<>();

    private GlMutableBuffer arenaBuffer;
    private GlMutableBuffer stagingBuffer;

    private int position;
    private int capacity;
    private int allocCount;

    public GlBufferArena(RenderDevice device, int initialSize) {
        this.device = device;

        try (CommandList commands = device.createCommandList()) {
            commands.allocateBuffer(GlBufferTarget.COPY_WRITE_BUFFER,
                    this.arenaBuffer = commands.createMutableBuffer(BUFFER_USAGE), initialSize);

            this.stagingBuffer = commands.createMutableBuffer(GlBufferUsage.GL_STATIC_DRAW);
        }

        this.resizeIncrement = initialSize;
        this.capacity = initialSize;
    }

    private void resize(CommandList commandList, int newCapacity) {
        GlMutableBuffer src = this.arenaBuffer;
        GlMutableBuffer dst = commandList.createMutableBuffer(BUFFER_USAGE);

        commandList.allocateBuffer(GlBufferTarget.COPY_WRITE_BUFFER, dst, newCapacity);
        commandList.copyBufferSubData(src, dst, 0, 0, this.position);
        commandList.deleteBuffer(src);

        this.arenaBuffer = dst;
        this.capacity = newCapacity;
    }

    public void checkArenaCapacity(CommandList commandList, int bytes) {
        if (this.position + bytes >= this.capacity) {
            this.resize(commandList, this.getNextSize(bytes));
        }
    }

    public GlBufferSegment uploadBuffer(CommandList commandList, ByteBuffer buffer) {
        int byteCount = buffer.remaining();

        this.checkArenaCapacity(commandList, byteCount);

        GlBufferSegment segment = this.alloc(byteCount);

        commandList.uploadData(this.stagingBuffer, buffer);
        commandList.copyBufferSubData(this.stagingBuffer, this.arenaBuffer, 0, segment.getStart(), byteCount);
        commandList.invalidateBuffer(this.stagingBuffer);

        return segment;
    }

    private int getNextSize(int len) {
        return Math.max(this.capacity + this.resizeIncrement, this.capacity + len);
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
            segment = new GlBufferSegment(this, this.position, len);

            this.position += len;
        }

        this.allocCount++;

        return segment;
    }

    private GlBufferSegment allocReuse(int len) {
        GlBufferSegment bestSegment = null;

        for (GlBufferSegment segment : this.freeRegions) {
            if (segment.getLength() < len) {
                continue;
            }

            if (bestSegment == null || bestSegment.getLength() > segment.getLength()) {
                bestSegment = segment;
            }
        }

        if (bestSegment == null) {
            return null;
        }

        this.freeRegions.remove(bestSegment);

        int excess = bestSegment.getLength() - len;

        if (excess > 0) {
            this.freeRegions.add(new GlBufferSegment(this, bestSegment.getStart() + len, excess));
        }

        return new GlBufferSegment(this, bestSegment.getStart(), len);
    }

    public void delete() {
        try (CommandList commands = this.device.createCommandList()) {
            commands.deleteBuffer(this.arenaBuffer);
        }
    }

    public boolean isEmpty() {
        return this.allocCount <= 0;
    }

    public GlBuffer getArenaBuffer() {
        return this.arenaBuffer;
    }
}
