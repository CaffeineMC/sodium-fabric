package me.jellysquid.mods.sodium.client.gl.buffer;

import org.lwjgl.opengl.ARBBufferStorage;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL44;
import org.lwjgl.opengl.GLCapabilities;

import java.nio.ByteBuffer;

public class GlImmutableBuffer extends GlBuffer {
    private static final BufferStorageFunctions storageFuncs = BufferStorageFunctions.pickBest(GL.getCapabilities());

    private final int flags;
    private boolean allocated;

    public GlImmutableBuffer(int flags) {
        this.flags = flags;
    }

    @Override
    public void upload(int target, BufferUploadData data) {
        if (this.allocated) {
            throw new IllegalStateException("Storage already allocated");
        }

        this.vertexCount = data.buffer.capacity() / data.format.getStride();

        storageFuncs.glBufferStorage(target, data.buffer, this.flags);

        this.allocated = true;
    }

    @Override
    public void allocate(int target, long size) {
        if (this.allocated) {
            throw new IllegalStateException("Storage already allocated");
        }

        storageFuncs.glBufferStorage(target, size, this.flags);

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
        return storageFuncs != BufferStorageFunctions.UNSUPPORTED;
    }

    private enum BufferStorageFunctions {
        CORE {
            @Override
            public void glBufferStorage(int target, ByteBuffer data, int flags) {
                GL44.glBufferStorage(target, data, flags);
            }

            @Override
            public void glBufferStorage(int target, long size, int flags) {
                GL44.glBufferStorage(target, size, flags);
            }
        },
        ARB {
            @Override
            public void glBufferStorage(int target, ByteBuffer data, int flags) {
                ARBBufferStorage.glBufferStorage(target, data, flags);
            }

            @Override
            public void glBufferStorage(int target, long size, int flags) {
                ARBBufferStorage.glBufferStorage(target, size, flags);
            }
        },
        UNSUPPORTED {
            @Override
            public void glBufferStorage(int target, ByteBuffer data, int flags) {
                throw new UnsupportedOperationException();
            }

            @Override
            public void glBufferStorage(int target, long size, int flags) {
                throw new UnsupportedOperationException();
            }
        };

        public abstract void glBufferStorage(int target, ByteBuffer data, int flags);
        public abstract void glBufferStorage(int target, long size, int flags);

        public static BufferStorageFunctions pickBest(GLCapabilities caps) {
            if (caps.OpenGL44) {
                return CORE;
            } else if (caps.GL_ARB_buffer_storage) {
                return ARB;
            } else {
                return UNSUPPORTED;
            }
        }
    }
}
