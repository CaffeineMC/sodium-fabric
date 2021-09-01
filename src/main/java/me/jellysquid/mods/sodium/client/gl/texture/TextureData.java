package me.jellysquid.mods.sodium.client.gl.texture;

import org.apache.commons.io.IOUtils;
import org.lwjgl.stb.STBImage;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

public class TextureData {
    public final ByteBuffer buffer;
    public final int width, height;

    public boolean disposed;

    private TextureData(ByteBuffer buffer, int width, int height) {
        this.buffer = buffer;
        this.width = width;
        this.height = height;
    }

    public static TextureData load(byte[] data) {
        ByteBuffer buf = MemoryUtil.memAlloc(data.length);
        buf.put(data);
        buf.flip();

        try (MemoryStack stack = MemoryStack.stackPush()) {
            var x = stack.callocInt(1);
            var y = stack.callocInt(1);
            var channels = stack.callocInt(1);

            ByteBuffer img = STBImage.stbi_load_from_memory(buf, x, y, channels, 4);

            if (img == null) {
                throw new RuntimeException("Failed to load image data: " + STBImage.stbi_failure_reason());
            }

            return new TextureData(img, x.get(0), y.get(0));
        }
    }

    public static TextureData loadInternal(String name) {
        try (InputStream in = TextureData.class.getResourceAsStream(name)) {
            if (in == null) {
                throw new FileNotFoundException();
            }

            return load(IOUtils.toByteArray(in));
        } catch (IOException e) {
            throw new RuntimeException("Failed to load internal texture", e);
        }
    }

    public void dispose() {
        STBImage.stbi_image_free(this.buffer);
        this.disposed = true;
    }

    public boolean isDisposed() {
        return this.disposed;
    }
}
