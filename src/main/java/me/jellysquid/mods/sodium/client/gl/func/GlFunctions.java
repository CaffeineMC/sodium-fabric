package me.jellysquid.mods.sodium.client.gl.func;

import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GLCapabilities;

public class GlFunctions {
    private static final GLCapabilities capabilities = GL.getCapabilities();

    public static final GlIndirectMultiDrawFunctions INDIRECT_DRAW = GlIndirectMultiDrawFunctions.load(capabilities);
    public static final GlInstancedArrayFunctions INSTANCED_ARRAY = GlInstancedArrayFunctions.load(capabilities);

    public static boolean isIndirectMultiDrawSupported() {
        return INDIRECT_DRAW != GlIndirectMultiDrawFunctions.UNSUPPORTED;
    }

    public static boolean isInstancedArraySupported() {
        return INSTANCED_ARRAY != GlInstancedArrayFunctions.UNSUPPORTED;
    }
}
