package me.jellysquid.mods.sodium.client.render.gl;

import me.jellysquid.mods.sodium.client.render.vertex.BufferUploadData;
import me.jellysquid.mods.sodium.client.render.vertex.ExtendedVertexFormat;
import net.minecraft.client.render.VertexFormat;

public class GlVertexArrayBuffer {
    private final VertexFormat format;

    private final GlVertexBuffer vertexBuffer;
    private final GlVertexArray vertexArray;

    private boolean init = false;

    public GlVertexArrayBuffer(VertexFormat format, GlVertexBuffer vertexBuffer, GlVertexArray vertexArray) {
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

    public void draw(int mode) {
        this.vertexBuffer.drawInline(mode);
    }

    public void upload(BufferUploadData buffer) {
        this.vertexBuffer.upload(buffer);
    }
}
