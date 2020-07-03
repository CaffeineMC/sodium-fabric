package me.jellysquid.mods.sodium.client.gl.arena;

import it.unimi.dsi.fastutil.objects.ObjectLinkedOpenHashSet;
import me.jellysquid.mods.sodium.client.gl.buffer.GlBuffer;
import me.jellysquid.mods.sodium.client.gl.buffer.GlMutableBuffer;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL31;
import org.lwjgl.opengl.GL33;

import java.util.Set;

public class GlBufferArena {
    private final int resizeIncrement;

    private final Set<GlBufferRegion> freeRegions = new ObjectLinkedOpenHashSet<>();

    private GlBuffer vertexBuffer;
    private boolean isBufferBound;

    private int position;
    private int capacity;
    private int allocCount;

    public GlBufferArena(int initialSize, int resizeIncrement) {
        this.vertexBuffer = this.createBuffer();
        this.vertexBuffer.bind(GL31.GL_COPY_WRITE_BUFFER);
        this.vertexBuffer.allocate(GL31.GL_COPY_WRITE_BUFFER, initialSize);
        this.vertexBuffer.unbind(GL31.GL_COPY_WRITE_BUFFER);

        this.resizeIncrement = resizeIncrement;
        this.capacity = initialSize;
    }

    private void resize(int size) {
        GlBuffer src = this.vertexBuffer;
        src.unbind(GL31.GL_COPY_WRITE_BUFFER);

        GlBuffer dst = this.createBuffer();

        GlBuffer.copy(src, dst, 0, 0, this.capacity, size);
        src.delete();

        dst.bind(GL31.GL_COPY_WRITE_BUFFER);

        this.vertexBuffer = dst;
        this.capacity = size;
    }

    private GlBuffer createBuffer() {
        return new GlMutableBuffer(GL15.GL_DYNAMIC_DRAW);
    }

    public void bind() {
        this.vertexBuffer.bind(GL31.GL_COPY_WRITE_BUFFER);
        this.isBufferBound = true;
    }

    public void ensureCapacity(int len) {
        if (this.position + len >= this.capacity) {
            this.resize(this.getNextSize(len));
        }
    }

    public GlBufferRegion upload(int readTarget, int offset, int len) {
        this.checkBufferBound();
        this.ensureCapacity(len);

        GlBufferRegion segment = this.alloc(len);
        GL33.glCopyBufferSubData(readTarget, GL31.GL_COPY_WRITE_BUFFER, offset, segment.getStart(), len);

        return segment;
    }

    public void unbind() {
        this.checkBufferBound();

        this.vertexBuffer.unbind(GL31.GL_COPY_WRITE_BUFFER);
        this.isBufferBound = false;
    }

    private int getNextSize(int len) {
        return Math.max(this.capacity + this.resizeIncrement, this.capacity + len);
    }

    public void free(GlBufferRegion segment) {
        if (!this.freeRegions.add(segment)) {
            throw new IllegalArgumentException("Segment already freed");
        }

        this.allocCount--;
    }

    private GlBufferRegion alloc(int len) {
        GlBufferRegion segment = this.allocReuse(len);

        if (segment == null) {
            segment = new GlBufferRegion(this, this.position, len);

            this.position += len;
        }

        this.allocCount++;

        return segment;
    }

    private GlBufferRegion allocReuse(int len) {
        GlBufferRegion bestSegment = null;

        for (GlBufferRegion segment : this.freeRegions) {
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
            this.freeRegions.add(new GlBufferRegion(this, bestSegment.getStart() + len, excess));
        }

        return new GlBufferRegion(this, bestSegment.getStart(), len);
    }

    public void delete() {
        this.vertexBuffer.delete();
    }

    public boolean isEmpty() {
        return this.allocCount <= 0;
    }

    public GlBuffer getBuffer() {
        return this.vertexBuffer;
    }

    private void checkBufferBound() {
        if (!this.isBufferBound) {
            throw new IllegalStateException("Buffer is not bound");
        }
    }
}
