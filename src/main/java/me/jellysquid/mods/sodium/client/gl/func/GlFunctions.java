package me.jellysquid.mods.sodium.client.gl.func;

import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GLCapabilities;

public class GlFunctions {
    private static final GLCapabilities capabilities = GL.getCapabilities();

    public static final GlVertexArrayFunctions VERTEX_ARRAY = GlVertexArrayFunctions.load(capabilities);
    public static final GlBufferCopyFunctions BUFFER_COPY = GlBufferCopyFunctions.load(capabilities);
    public static final GlBufferStorageFunctions BUFFER_STORAGE = GlBufferStorageFunctions.load(capabilities);

    private static final boolean SHADER_DRAW_PARAMETERS_SUPPORTED = capabilities.OpenGL46 || capabilities.GL_ARB_shader_draw_parameters;

    public static boolean isVertexArraySupported() {
        return VERTEX_ARRAY != GlVertexArrayFunctions.UNSUPPORTED;
    }

    public static boolean isBufferCopySupported() {
        return BUFFER_COPY != GlBufferCopyFunctions.UNSUPPORTED;
    }

    public static boolean isBufferStorageSupported() {
        return BUFFER_STORAGE != GlBufferStorageFunctions.UNSUPPORTED;
    }

    public static boolean isShaderDrawParametersSupported() {
        return SHADER_DRAW_PARAMETERS_SUPPORTED;
    }
}
