package me.jellysquid.mods.sodium.client.gl.buffer;

import me.jellysquid.mods.sodium.client.gl.func.GlFunctions;
import org.lwjgl.opengl.GL44;

import java.nio.ByteBuffer;

/**
 * An "immutable" buffer type which cannot have its storage modified after it has been created. This means that once the
 * buffer has been allocated, it cannot be re-allocated without destroying the buffer and creating a new one.
 *
 * The contents of the buffer's storage can be modified using normal GL code.
 */
public class GlImmutableBuffer extends GlBuffer {
    private final int flags;
    private boolean allocated;

    public GlImmutableBuffer(int flags) {
        if (!isSupported()) {
            throw new UnsupportedOperationException("Buffer storage is not supported on this platform");
        }

        this.flags = flags;
    }

    @Override
    public void upload(int target, VertexData data) {
        if (this.allocated) {
            throw new IllegalStateException("Storage already allocated");
        }

        this.vertexCount = data.buffer.capacity() / data.format.getStride();

        GlFunctions.BUFFER_STORAGE.glBufferStorage(target, data.buffer, this.flags);

        this.allocated = true;
    }

    @Override
    public void allocate(int target, long size) {
        if (this.allocated) {
            throw new IllegalStateException("Storage already allocated");
        }

        GlFunctions.BUFFER_STORAGE.glBufferStorage(target, size, this.flags);

        this.allocated = true;
    }

    @Override
    public void uploadSub(int target, int offset, ByteBuffer data) {
        if ((this.flags & GL44.GL_DYNAMIC_STORAGE_BIT) == 0) {
            throw new IllegalStateException("Storage is not dynamic");
        }

        super.uploadSub(target, offset, data);
    }

    public static boolean isSupported() {
        return GlFunctions.isBufferStorageSupported();
    }

}
