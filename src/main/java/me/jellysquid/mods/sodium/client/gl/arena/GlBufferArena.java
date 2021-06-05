package me.jellysquid.mods.sodium.client.gl.arena;

import it.unimi.dsi.fastutil.objects.ObjectLinkedOpenHashSet;
import me.jellysquid.mods.sodium.client.gl.buffer.GlBuffer;
import me.jellysquid.mods.sodium.client.gl.buffer.GlBufferTarget;
import me.jellysquid.mods.sodium.client.gl.buffer.GlBufferUsage;
import me.jellysquid.mods.sodium.client.gl.buffer.GlMutableBuffer;
import me.jellysquid.mods.sodium.client.gl.device.CommandList;
import me.jellysquid.mods.sodium.client.gl.device.RenderDevice;

import java.util.Set;

public class GlBufferArena {
    private static final GlBufferUsage BUFFER_USAGE = GlBufferUsage.GL_DYNAMIC_DRAW;

    private final RenderDevice device;
    private final int resizeIncrement;

    private final Set<GlBufferSegment> freeRegions = new ObjectLinkedOpenHashSet<>();

    private GlMutableBuffer vertexBuffer;

    private int position;
    private int capacity;
    private int allocCount;

    public GlBufferArena(RenderDevice device, int initialSize, int resizeIncrement) {
        this.device = device;

        try (CommandList commands = device.createCommandList()) {
            this.vertexBuffer = commands.createMutableBuffer(BUFFER_USAGE);
            commands.allocateBuffer(GlBufferTarget.COPY_WRITE_BUFFER, this.vertexBuffer, initialSize);
        }

        this.resizeIncrement = resizeIncrement;
        this.capacity = initialSize;
    }

    private void resize(CommandList commandList, int newCapacity) {
        GlMutableBuffer src = this.vertexBuffer;
        GlMutableBuffer dst = commandList.createMutableBuffer(BUFFER_USAGE);

        commandList.allocateBuffer(GlBufferTarget.COPY_WRITE_BUFFER, dst, newCapacity);
        commandList.copyBufferSubData(src, dst, 0, 0, this.position);
        commandList.deleteBuffer(src);

        this.vertexBuffer = dst;
        this.capacity = newCapacity;
    }

    public void prepareBuffer(CommandList commandList, int bytes) {
        if (this.position + bytes >= this.capacity) {
            this.resize(commandList, this.getNextSize(bytes));
        }
    }

    public GlBufferSegment uploadBuffer(CommandList commandList, GlBuffer readBuffer, int readOffset, int byteCount) {
        this.prepareBuffer(commandList, byteCount);

        GlBufferSegment segment = this.alloc(byteCount);

        commandList.copyBufferSubData(readBuffer, this.vertexBuffer, readOffset, segment.getStart(), byteCount);

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
            commands.deleteBuffer(this.vertexBuffer);
        }
    }

    public boolean isEmpty() {
        return this.allocCount <= 0;
    }

    public GlBuffer getBuffer() {
        return this.vertexBuffer;
    }
}
