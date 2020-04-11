package me.jellysquid.mods.sodium.client.gl.buffer;

import org.lwjgl.opengl.GL15;

public class GlMutableBuffer extends GlBuffer {
    public GlMutableBuffer(int target) {
        super(target);
    }

    @Override
    public void upload(BufferUploadData data) {
        this.vertexFormat = data.format;
        this.vertexCount = data.buffer.remaining() / data.format.getVertexSize();

        bufferFuncs.glBufferData(this.target, data.buffer, GL15.GL_STATIC_DRAW);
    }
}
