package me.jellysquid.mods.sodium.client.gl.buffer;

import net.minecraft.client.render.VertexFormat;
import org.lwjgl.opengl.GL15;

import java.nio.ByteBuffer;

public class GlMutableBuffer extends GlBuffer {
    public GlMutableBuffer(int target) {
        super(target);
    }

    public void upload(BufferUploadData data) {
        ByteBuffer buffer = data.buffer;
        VertexFormat format = data.format;

        this.vertexFormat = format;
        this.vertexCount = buffer.remaining() / format.getVertexSize();

        bufferFuncs.glBufferData(this.target, buffer, GL15.GL_STATIC_DRAW);
    }
}
