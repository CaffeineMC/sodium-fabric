package me.jellysquid.mods.sodium.client.gl.func;

import org.lwjgl.opengl.ARBCopyBuffer;
import org.lwjgl.opengl.GL31C;
import org.lwjgl.opengl.GLCapabilities;

/**
 * Requires OpenGL 3.1+ or the ARB_copy_buffer extension.
 */
public enum GlBufferCopyFunctions {
    CORE {
        @Override
        public void glCopyBufferSubData(int readTarget, int writeTarget, long readOffset, long writeOffset, long size) {
            GL31C.glCopyBufferSubData(readTarget, writeTarget, readOffset, writeOffset, size);
        }
    },
    ARB {
        @Override
        public void glCopyBufferSubData(int readTarget, int writeTarget, long readOffset, long writeOffset, long size) {
            ARBCopyBuffer.glCopyBufferSubData(readTarget, writeTarget, readOffset, writeOffset, size);
        }
    },
    UNSUPPORTED {
        @Override
        public void glCopyBufferSubData(int readTarget, int writeTarget, long readOffset, long writeOffset, long size) {
            throw new UnsupportedOperationException();
        }
    };

    static GlBufferCopyFunctions load(GLCapabilities capabilities) {
        if (capabilities.OpenGL31) {
            return GlBufferCopyFunctions.CORE;
        } else if (capabilities.GL_ARB_copy_buffer) {
            return GlBufferCopyFunctions.ARB;
        } else {
            return GlBufferCopyFunctions.UNSUPPORTED;
        }
    }

    public abstract void glCopyBufferSubData(int readTarget, int writeTarget, long readOffset, long writeOffset, long size);
}
