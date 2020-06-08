package me.jellysquid.mods.sodium.client.gl.buffer;

import me.jellysquid.mods.sodium.client.gl.GlObject;
import me.jellysquid.mods.sodium.client.gl.func.GlFunctions;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL31;

import java.nio.ByteBuffer;

public abstract class GlBuffer extends GlObject {
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
        this.drawArrays(mode, 0, this.vertexCount);
    }

    public void drawArrays(int mode, int first, int count) {
        GL11.glDrawArrays(mode, first, count);
    }

    public abstract void allocate(int target, long size);

    public abstract void upload(int target, VertexData data);

    public void delete() {
        GL15.glDeleteBuffers(this.handle());

        this.invalidateHandle();
    }

    public void uploadSub(int target, int offset, ByteBuffer data) {
        GL15.glBufferSubData(target, offset, data);
    }

    public static void copy(GlBuffer src, GlBuffer dst, int readOffset, int writeOffset, int copyLen, int bufferSize) {
        src.bind(GL31.GL_COPY_READ_BUFFER);

        dst.bind(GL31.GL_COPY_WRITE_BUFFER);
        dst.allocate(GL31.GL_COPY_WRITE_BUFFER, bufferSize);

        GlFunctions.BUFFER_COPY.glCopyBufferSubData(GL31.GL_COPY_READ_BUFFER, GL31.GL_COPY_WRITE_BUFFER, readOffset, writeOffset, copyLen);

        dst.unbind(GL31.GL_COPY_WRITE_BUFFER);
        src.unbind(GL31.GL_COPY_READ_BUFFER);
    }
}
