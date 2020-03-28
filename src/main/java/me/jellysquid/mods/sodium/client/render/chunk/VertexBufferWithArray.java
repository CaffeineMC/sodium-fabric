package me.jellysquid.mods.sodium.client.render.chunk;

import me.jellysquid.mods.sodium.client.render.gl.GlVertexArray;
import me.jellysquid.mods.sodium.client.render.gl.GlVertexBuffer;
import me.jellysquid.mods.sodium.client.render.vertex.BufferUploadData;
import me.jellysquid.mods.sodium.client.render.vertex.ExtendedVertexFormat;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.util.math.Matrix4f;

public class VertexBufferWithArray {
    private final VertexFormat format;

    private final GlVertexBuffer vertexBuffer;
    private final GlVertexArray vertexArray;

    private boolean init = false;

    public VertexBufferWithArray(VertexFormat format, GlVertexBuffer vertexBuffer, GlVertexArray vertexArray) {
        this.vertexBuffer = vertexBuffer;
        this.vertexArray = vertexArray;

        this.format = format;
    }

    public void unbind() {
        GlVertexArray.unbind();
    }

    public void delete() {
        this.vertexBuffer.delete();
        this.vertexArray.delete();
    }

    public void bind() {
        this.vertexArray.bind();

        if (!this.init) {
            this.setup();

            this.init = true;
        }
    }

    private void setup() {
        this.vertexBuffer.bind();
        ((ExtendedVertexFormat) this.format).setupVertexArrayState(0L);
        this.vertexBuffer.unbind();
    }

    public void draw(Matrix4f modelMatrix, int mode) {
        this.vertexBuffer.draw(modelMatrix, mode);
    }

    public void upload(BufferUploadData buffer) {
        this.vertexBuffer.upload(buffer);
    }
}
