package me.jellysquid.mods.sodium.client.gl;

import com.mojang.blaze3d.platform.GlStateManager;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormatElement;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL30;

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

        int size = this.format.getVertexSize();
        int pointer = 0;

        for (VertexFormatElement element : this.format.getElements()) {
            setupVertexArrayState(element.getIndex(), element.getType(), element.getFormat(), element.getCount(), pointer, size);
            pointer += element.getSize();
        }

        this.vertexBuffer.unbind();
    }

    public void draw(int mode) {
        this.vertexBuffer.drawInline(mode);
    }

    public void upload(BufferUploadData buffer) {
        this.vertexBuffer.upload(buffer);
    }

    private static void setupVertexArrayState(int index, VertexFormatElement.Type type, VertexFormatElement.Format format, int count, long pointer, int stride) {
        switch (type) {
            case POSITION:
                GlStateManager.vertexPointer(count, format.getGlId(), stride, pointer);
                GL30.glEnableClientState(GL11.GL_VERTEX_ARRAY);
                break;
            case NORMAL:
                GlStateManager.normalPointer(format.getGlId(), stride, pointer);
                GL30.glEnableClientState(GL11.GL_NORMAL_ARRAY);
                break;
            case COLOR:
                GlStateManager.colorPointer(count, format.getGlId(), stride, pointer);
                GL30.glEnableClientState(GL11.GL_COLOR_ARRAY);
                break;
            case UV:
                GlStateManager.clientActiveTexture(GL13.GL_TEXTURE0 + index);
                GlStateManager.texCoordPointer(count, format.getGlId(), stride, pointer);
                GL30.glEnableClientState(GL11.GL_TEXTURE_COORD_ARRAY);
                break;
            case PADDING:
                break;
            default:
                throw new UnsupportedOperationException("Type does not support setting up a Vertex Array state");
        }
    }
}
