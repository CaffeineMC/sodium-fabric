package me.jellysquid.mods.sodium.client.gl.util;

import org.lwjgl.opengl.GL11;

import java.util.Objects;

public class GlVendorUtil {
    public static boolean matches(String vendor) {
        return Objects.equals(GL11.glGetString(GL11.GL_VENDOR), vendor);
    }
}
