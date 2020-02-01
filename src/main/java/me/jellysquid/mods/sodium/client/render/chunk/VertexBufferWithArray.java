package me.jellysquid.mods.sodium.client.render.chunk;

import me.jellysquid.mods.sodium.client.render.gl.GlVertexArray;
import me.jellysquid.mods.sodium.client.render.vertex.ExtendedVertexFormat;
import net.minecraft.client.gl.VertexBuffer;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.util.math.Matrix4f;

public class VertexBufferWithArray {
    private final VertexFormat format;

    private final VertexBuffer vertexBuffer;
    private final GlVertexArray vertexArray;

    private boolean init = false;

    public VertexBufferWithArray(VertexFormat format, VertexBuffer vertexBuffer, GlVertexArray vertexArray) {
        this.vertexBuffer = vertexBuffer;
        this.vertexArray = vertexArray;

        this.format = format;
    }

    public static void unbind() {
        GlVertexArray.unbind();
    }

    public void delete() {
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
        VertexBuffer.unbind();
    }

    public void draw(Matrix4f modelMatrix, int mode) {
        this.vertexBuffer.draw(modelMatrix, mode);
    }
}
