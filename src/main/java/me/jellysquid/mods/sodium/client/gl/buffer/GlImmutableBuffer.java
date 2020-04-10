package me.jellysquid.mods.sodium.client.gl.buffer;

import org.lwjgl.opengl.ARBBufferStorage;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL44;
import org.lwjgl.opengl.GLCapabilities;

import java.nio.ByteBuffer;

public class GlImmutableBuffer extends GlBuffer {
    private static final BufferStorageFunctions storageFuncs = BufferStorageFunctions.pickBest(GL.getCapabilities());

    public GlImmutableBuffer(int target) {
        super(target);
    }

    @Override
    public void upload(BufferUploadData data) {
        this.vertexFormat = data.format;
        this.vertexCount = data.buffer.capacity() / data.format.getVertexSize();

        storageFuncs.glBufferStorage(this.target, data.buffer, 0);
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
        },
        ARB {
            @Override
            public void glBufferStorage(int target, ByteBuffer data, int flags) {
                ARBBufferStorage.glBufferStorage(target, data, flags);
            }
        },
        UNSUPPORTED {
            @Override
            public void glBufferStorage(int target, ByteBuffer data, int flags) {
                throw new UnsupportedOperationException();
            }
        };

        public abstract void glBufferStorage(int target, ByteBuffer data, int flags);

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
