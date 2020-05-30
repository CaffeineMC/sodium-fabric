package me.jellysquid.mods.sodium.client.gl.func;

import org.lwjgl.opengl.ARBBufferStorage;
import org.lwjgl.opengl.GL44;
import org.lwjgl.opengl.GLCapabilities;

import java.nio.ByteBuffer;

/**
 * Requires OpenGL 4.4+ or the ARB_buffer_storage extension.
 */
public enum GlBufferStorageFunctions {
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

    static GlBufferStorageFunctions load(GLCapabilities caps) {
        if (caps.OpenGL44) {
            return CORE;
        } else if (caps.GL_ARB_buffer_storage) {
            return ARB;
        } else {
            return UNSUPPORTED;
        }
    }
}
