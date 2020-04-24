package me.jellysquid.mods.sodium.client.gl.buffer;

import me.jellysquid.mods.sodium.client.gl.GlHandle;
import org.lwjgl.opengl.*;

import java.nio.ByteBuffer;

public abstract class GlBuffer extends GlHandle {
    public static final CopyBufferFunctions copyBufferFuncs = CopyBufferFunctions.pickBest(GL.getCapabilities());

    protected int vertexCount = 0;

    protected GlBuffer() {
        this.setHandle(GL15.glGenBuffers());
    }

    public void unbind(int target) {
        GL15.glBindBuffer(target, 0);
    }

    public void bind(int target) {
        GL15.glBindBuffer(target, this.handle());
    }

    public void drawArrays(int mode) {
        GL11.glDrawArrays(mode, 0, this.vertexCount);
    }

    public abstract void upload(int target, BufferUploadData data);

    public void delete() {
        GL15.glDeleteBuffers(this.handle());

        this.invalidateHandle();
    }

    public abstract void allocate(int target, long size);

    public static void copy(GlBuffer src, GlBuffer dst, int readOffset, int writeOffset, int copyLen, int bufferSize) {
        src.bind(GL31.GL_COPY_READ_BUFFER);

        dst.bind(GL31.GL_COPY_WRITE_BUFFER);
        dst.allocate(GL31.GL_COPY_WRITE_BUFFER, bufferSize);

        copyBufferFuncs.glCopyBufferSubData(GL31.GL_COPY_READ_BUFFER, GL31.GL_COPY_WRITE_BUFFER, readOffset, writeOffset, copyLen);

        dst.unbind(GL31.GL_COPY_WRITE_BUFFER);
        src.unbind(GL31.GL_COPY_READ_BUFFER);
    }

    public void uploadSub(int target, int offset, ByteBuffer data) {
        GL15.glBufferSubData(target, offset, data);
    }

    public static boolean isBufferCopySupported() {
        return copyBufferFuncs != CopyBufferFunctions.UNSUPPORTED;
    }

    private enum CopyBufferFunctions {
        CORE {
            @Override
            public void glCopyBufferSubData(int readTarget, int writeTarget, long readOffset, long writeOffset, long size) {
                GL31.glCopyBufferSubData(readTarget, writeTarget, readOffset, writeOffset, size);
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

        public static CopyBufferFunctions pickBest(GLCapabilities capabilities) {
            if (capabilities.OpenGL31) {
                return CopyBufferFunctions.CORE;
            } else if (capabilities.GL_ARB_copy_buffer) {
                return CopyBufferFunctions.ARB;
            } else {
                return CopyBufferFunctions.UNSUPPORTED;
            }
        }

        public abstract void glCopyBufferSubData(int readTarget, int writeTarget, long readOffset, long writeOffset, long size);
    }
}
