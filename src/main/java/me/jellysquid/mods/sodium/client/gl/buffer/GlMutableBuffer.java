package me.jellysquid.mods.sodium.client.gl.buffer;

import org.lwjgl.opengl.GL15;

public class GlMutableBuffer extends GlBuffer {
    private final int hints;

    public GlMutableBuffer(int hints) {
        this.hints = hints;
    }

    @Override
    public void upload(int target, BufferUploadData data) {
        this.vertexCount = data.buffer.remaining() / data.format.getStride();

        GL15.glBufferData(target, data.buffer, this.hints);
    }

    @Override
    public void allocate(int target, long size) {
        GL15.glBufferData(target, size, this.hints);
    }
}
