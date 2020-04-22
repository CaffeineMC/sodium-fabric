package me.jellysquid.mods.sodium.client.render.backends.shader.lcb;

import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import me.jellysquid.mods.sodium.client.gl.array.GlVertexArray;
import me.jellysquid.mods.sodium.client.gl.attribute.GlVertexAttributeBinding;
import me.jellysquid.mods.sodium.client.gl.buffer.BufferUploadData;
import me.jellysquid.mods.sodium.client.gl.buffer.GlBuffer;
import me.jellysquid.mods.sodium.client.gl.buffer.GlImmutableBuffer;
import me.jellysquid.mods.sodium.client.gl.buffer.GlMutableBuffer;
import net.minecraft.util.math.ChunkSectionPos;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL44;

import java.nio.ByteBuffer;
import java.util.Set;

public class BufferBlock {
    private static final int DEFAULT_SIZE = 128 * 1024;
    private static final int DEFAULT_RESIZE_INCREMENT = 512 * 1024;

    private final ChunkSectionPos origin;
    private final boolean useImmutableStorage;

    private final Set<BufferSegment> freeSegments = new ObjectOpenHashSet<>();
    private final GlVertexArray vertexArray;
    private GlBuffer vertexBuffer;

    private boolean vertexArraySetup;

    private int position;
    private int capacity;
    private int allocCount;

    public BufferBlock(ChunkSectionPos origin, boolean useImmutableStorage) {
        this.useImmutableStorage = useImmutableStorage;
        this.origin = origin;
        this.vertexArray = new GlVertexArray();

        this.vertexBuffer = this.createBuffer();
        this.vertexBuffer.bind(GL15.GL_ARRAY_BUFFER);
        this.vertexBuffer.allocate(GL15.GL_ARRAY_BUFFER, DEFAULT_SIZE);
        this.vertexBuffer.unbind(GL15.GL_ARRAY_BUFFER);

        this.capacity = DEFAULT_SIZE;
    }

    private void resize(int size) {
        GlBuffer src = this.vertexBuffer;
        src.unbind(GL15.GL_ARRAY_BUFFER);

        GlBuffer dst = this.createBuffer();
        GlBuffer.copy(src, dst, 0, 0, this.capacity, size);

        dst.bind(GL15.GL_ARRAY_BUFFER);

        this.vertexBuffer = dst;
        this.capacity = size;
        this.vertexArraySetup = false;
    }

    private GlBuffer createBuffer() {
        return this.useImmutableStorage ? new GlImmutableBuffer(GL44.GL_DYNAMIC_STORAGE_BIT) : new GlMutableBuffer(GL15.GL_STATIC_DRAW);
    }

    public void bind(GlVertexAttributeBinding[] attributes) {
        this.vertexArray.bind();

        if (!this.vertexArraySetup) {
            this.setupVertexArrayState(attributes);
        }
    }

    private void setupVertexArrayState(GlVertexAttributeBinding[] attributes) {
        this.vertexBuffer.bind(GL15.GL_ARRAY_BUFFER);

        for (GlVertexAttributeBinding binding : attributes) {
            GL20.glVertexAttribPointer(binding.index, binding.count, binding.format, binding.normalized, binding.stride, binding.pointer);
            GL20.glEnableVertexAttribArray(binding.index);
        }

        this.vertexBuffer.unbind(GL15.GL_ARRAY_BUFFER);
        this.vertexArraySetup = true;
    }

    public void unbind() {
        this.vertexArray.unbind();
    }

    public void beginUpload() {
        this.vertexBuffer.bind(GL15.GL_ARRAY_BUFFER);
    }

    public void endUploads() {
        this.vertexBuffer.unbind(GL15.GL_ARRAY_BUFFER);
    }

    public BufferSegment upload(BufferUploadData data) {
        ByteBuffer buf = data.buffer;
        int len = buf.capacity();

        BufferSegment segment = this.alloc(len);

        if (this.position >= this.capacity) {
            this.resize(this.getNextSize(len));
        }

        this.vertexBuffer.uploadSub(GL15.GL_ARRAY_BUFFER, segment.getStart(), buf);

        return segment;
    }

    private int getNextSize(int len) {
        return Math.max(this.capacity + DEFAULT_RESIZE_INCREMENT, this.capacity + len);
    }

    public void free(BufferSegment segment) {
        if (!this.freeSegments.add(segment)) {
            throw new IllegalArgumentException("Segment already freed");
        }

        this.allocCount--;
    }

    private BufferSegment alloc(int len) {
        BufferSegment segment = this.allocReuse(len);

        if (segment == null) {
            segment = new BufferSegment(this, this.position, len);

            this.position += len;
        }

        this.allocCount++;

        return segment;
    }

    private BufferSegment allocReuse(int len) {
        BufferSegment bestSegment = null;

        for (BufferSegment segment : this.freeSegments) {
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

        this.freeSegments.remove(bestSegment);

        int excess = bestSegment.getLength() - len;

        if (excess > 0) {
            this.freeSegments.add(new BufferSegment(this, bestSegment.getStart() + len, excess));
        }

        return new BufferSegment(this, bestSegment.getStart(), len);
    }

    public void delete() {
        this.vertexBuffer.delete();
        this.vertexArray.delete();
    }

    public ChunkSectionPos getOrigin() {
        return this.origin;
    }

    public boolean isEmpty() {
        return this.allocCount <= 0;
    }
}
