package me.jellysquid.mods.sodium.client.gl;

import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GLCapabilities;

public class GlHelper {
    private static final boolean supportsNvFogB;

    static {
        GLCapabilities caps = GL.getCapabilities();
        supportsNvFogB = caps.GL_NV_fog_distance;
    }

    public static boolean supportsNvFog() {
        return supportsNvFogB;
    }
}
