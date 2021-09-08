package me.jellysquid.mods.thingl.functions;

import me.jellysquid.mods.thingl.buffer.BufferStorageFlags;
import me.jellysquid.mods.thingl.buffer.BufferTarget;
import me.jellysquid.mods.thingl.buffer.BufferUsage;
import me.jellysquid.mods.thingl.device.RenderDeviceImpl;
import me.jellysquid.mods.thingl.util.EnumBitField;
import org.lwjgl.opengl.ARBBufferStorage;
import org.lwjgl.opengl.ARBDirectStateAccess;
import org.lwjgl.opengl.GL45C;
import org.lwjgl.opengl.GLCapabilities;

import java.nio.ByteBuffer;

public enum DirectStateAccessFunctions {
    CORE {
        @Override
        public int createVertexArrays() {
            return GL45C.glCreateVertexArrays();
        }

        @Override
        public void vertexArrayVertexBuffer(int vertexArray, int bufferIndex, int buffer, int offset, int stride) {
            GL45C.glVertexArrayVertexBuffer(vertexArray, bufferIndex, buffer, offset, stride);
        }

        @Override
        public void vertexArrayAttribBinding(int vertexArray, int attributeIndex, int bufferIndex) {
            GL45C.glVertexArrayAttribBinding(vertexArray, attributeIndex, bufferIndex);
        }

        @Override
        public void vertexArrayAttribFormat(int vertexArray, int attributeIndex, int attributeSize, int attributeFormat, boolean attributeNormalized, int attributeOffset) {
            GL45C.glVertexArrayAttribFormat(vertexArray, attributeIndex, attributeSize, attributeFormat, attributeNormalized, attributeOffset);
        }

        @Override
        public void enableVertexArrayAttrib(int vertexArray, int attributeIndex) {
            GL45C.glEnableVertexArrayAttrib(vertexArray, attributeIndex);
        }

        @Override
        public void vertexArrayElementBuffer(int vertexArray, int buffer) {
            GL45C.glVertexArrayElementBuffer(vertexArray, buffer);
        }

        @Override
        public void vertexArrayAttribIFormat(int vertexArray, int attributeIndex, int attributeSize, int attributeFormat, int attributeOffset) {
            GL45C.glVertexArrayAttribIFormat(vertexArray, attributeIndex, attributeSize, attributeFormat, attributeOffset);
        }

        @Override
        public int createBuffers() {
            return GL45C.glCreateBuffers();
        }

        @Override
        public void namedBufferData(int buffer, ByteBuffer data, int usage) {
            GL45C.glNamedBufferData(buffer, data, usage);
        }

        @Override
        public void createNamedBufferStorage(int buffer, long size, EnumBitField<BufferStorageFlags> flags) {
            GL45C.glNamedBufferStorage(buffer, size, flags.getBitField());
        }

        @Override
        public void copyNamedBufferSubData(int srcBuffer, int dstBuffer, long readOffset, long writeOffset, long bytes) {
            GL45C.glCopyNamedBufferSubData(srcBuffer, dstBuffer, readOffset, writeOffset, bytes);
        }

        @Override
        public ByteBuffer mapNamedBufferRange(int buffer, long offset, long length, int flags) {
            return GL45C.glMapNamedBufferRange(buffer, offset, length, flags);
        }

        @Override
        public void unmapNamedBuffer(int buffer) {
            GL45C.glUnmapNamedBuffer(buffer);
        }
    },
    ARB {
        @Override
        public int createVertexArrays() {
            return ARBDirectStateAccess.glCreateVertexArrays();
        }

        @Override
        public void vertexArrayVertexBuffer(int vertexArray, int bufferIndex, int buffer, int offset, int stride) {
            ARBDirectStateAccess.glVertexArrayVertexBuffer(vertexArray, bufferIndex, buffer, offset, stride);
        }

        @Override
        public void vertexArrayAttribBinding(int vertexArray, int attributeIndex, int bufferIndex) {
            ARBDirectStateAccess.glVertexArrayAttribBinding(vertexArray, attributeIndex, bufferIndex);
        }

        @Override
        public void vertexArrayAttribFormat(int vertexArray, int attributeIndex, int attributeSize, int attributeFormat, boolean attributeNormalized, int attributeOffset) {
            ARBDirectStateAccess.glVertexArrayAttribFormat(vertexArray, attributeIndex, attributeSize, attributeFormat, attributeNormalized, attributeOffset);
        }

        @Override
        public void enableVertexArrayAttrib(int vertexArray, int attributeIndex) {
            ARBDirectStateAccess.glEnableVertexArrayAttrib(vertexArray, attributeIndex);
        }

        @Override
        public void vertexArrayElementBuffer(int vertexArray, int buffer) {
            ARBDirectStateAccess.glVertexArrayElementBuffer(vertexArray, buffer);
        }

        @Override
        public void vertexArrayAttribIFormat(int vertexArray, int attributeIndex, int attributeSize, int attributeFormat, int attributeOffset) {
            ARBDirectStateAccess.glVertexArrayAttribIFormat(vertexArray, attributeIndex, attributeSize, attributeFormat, attributeOffset);
        }

        @Override
        public int createBuffers() {
            return ARBDirectStateAccess.glCreateBuffers();
        }

        @Override
        public void namedBufferData(int buffer, ByteBuffer data, int usage) {
            ARBDirectStateAccess.glNamedBufferData(buffer, data, usage);
        }

        @Override
        public void createNamedBufferStorage(int buffer, long size, EnumBitField<BufferStorageFlags> flags) {
            ARBDirectStateAccess.glNamedBufferStorage(buffer, size, flags.getBitField());
        }

        @Override
        public void copyNamedBufferSubData(int srcBuffer, int dstBuffer, long readOffset, long writeOffset, long bytes) {
            ARBDirectStateAccess.glCopyNamedBufferSubData(srcBuffer, dstBuffer, readOffset, writeOffset, bytes);
        }

        @Override
        public ByteBuffer mapNamedBufferRange(int buffer, long offset, long length, int flags) {
            return ARBDirectStateAccess.glMapNamedBufferRange(buffer, offset, length, flags);
        }

        @Override
        public void unmapNamedBuffer(int buffer) {
            ARBDirectStateAccess.glUnmapNamedBuffer(buffer);
        }
    },
    NONE {
        @Override
        public int createVertexArrays() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void vertexArrayVertexBuffer(int vertexArray, int bufferIndex, int buffer, int offset, int stride) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void vertexArrayAttribBinding(int vertexArray, int attributeIndex, int bufferIndex) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void vertexArrayAttribFormat(int vertexArray, int attributeIndex, int attributeSize, int attributeFormat, boolean attributeNormalized, int attributeOffset) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void enableVertexArrayAttrib(int vertexArray, int attributeIndex) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void vertexArrayElementBuffer(int vertexArray, int buffer) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void vertexArrayAttribIFormat(int vertexArray, int attributeIndex, int attributeSize, int attributeFormat, int attributeOffset) {
            throw new UnsupportedOperationException();
        }

        @Override
        public int createBuffers() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void namedBufferData(int buffer, ByteBuffer data, int usage) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void createNamedBufferStorage(int buffer, long size, EnumBitField<BufferStorageFlags> flags) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void copyNamedBufferSubData(int srcBuffer, int dstBuffer, long readOffset, long writeOffset, long bytes) {
            throw new UnsupportedOperationException();
        }

        @Override
        public ByteBuffer mapNamedBufferRange(int buffer, long offset, long length, int flags) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void unmapNamedBuffer(int buffer) {
            throw new UnsupportedOperationException();
        }
    };

    public static DirectStateAccessFunctions pickBest(RenderDeviceImpl device) {
        GLCapabilities capabilities = device.getCapabilities();

        if (capabilities.OpenGL45) {
            return CORE;
        } else if (capabilities.GL_ARB_direct_state_access) {
            return ARB;
        } else {
            return NONE;
        }
    }

    public abstract int createVertexArrays();

    public abstract void vertexArrayVertexBuffer(int vertexArray, int bufferIndex, int buffer, int offset, int stride);

    public abstract void vertexArrayAttribBinding(int vertexArray, int attributeIndex, int bufferIndex);

    public abstract void vertexArrayAttribFormat(int vertexArray, int attributeIndex, int attributeSize,
                                                 int attributeFormat, boolean attributeNormalized, int attributeOffset);

    public abstract void enableVertexArrayAttrib(int vertexArray, int attributeIndex);

    public abstract void vertexArrayElementBuffer(int vertexArray, int buffer);

    public abstract void vertexArrayAttribIFormat(int vertexArray, int attributeIndex, int attributeSize,
                                                  int attributeFormat, int attributeOffset);

    public abstract int createBuffers();

    public abstract void namedBufferData(int buffer, ByteBuffer data, int usage);

    public abstract void createNamedBufferStorage(int buffer, long size, EnumBitField<BufferStorageFlags> flags);

    public abstract void copyNamedBufferSubData(int srcBuffer, int dstBuffer, long readOffset, long writeOffset, long bytes);

    public abstract ByteBuffer mapNamedBufferRange(int buffer, long offset, long length, int flags);

    public abstract void unmapNamedBuffer(int buffer);
}
