package me.jellysquid.mods.sodium.client.gl;

import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GLCapabilities;

public class GlHelper {
    private static final IntSet SUPPORTED_VERTEX_FORMATS = new IntOpenHashSet();

    static {
        GLCapabilities caps = GL.getCapabilities();

        if (caps.OpenGL11) {
            SUPPORTED_VERTEX_FORMATS.add(GL11.GL_BYTE);
            SUPPORTED_VERTEX_FORMATS.add(GL11.GL_UNSIGNED_BYTE);
            SUPPORTED_VERTEX_FORMATS.add(GL11.GL_SHORT);
            SUPPORTED_VERTEX_FORMATS.add(GL11.GL_UNSIGNED_SHORT);
            SUPPORTED_VERTEX_FORMATS.add(GL11.GL_INT);
            SUPPORTED_VERTEX_FORMATS.add(GL11.GL_UNSIGNED_INT);
            SUPPORTED_VERTEX_FORMATS.add(GL11.GL_FLOAT);
        }

        if (caps.OpenGL30) {
            SUPPORTED_VERTEX_FORMATS.add(GL30.GL_HALF_FLOAT);
        }
    }

    public static boolean isVertexFormatSupported(int format) {
        return SUPPORTED_VERTEX_FORMATS.contains(format);
    }
}
