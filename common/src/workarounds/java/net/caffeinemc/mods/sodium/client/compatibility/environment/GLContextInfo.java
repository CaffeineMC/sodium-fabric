package net.caffeinemc.mods.sodium.client.compatibility.environment;

import org.jetbrains.annotations.Nullable;
import org.lwjgl.opengl.GL11C;

public record GLContextInfo(String vendor, String renderer, String version) {
    @Nullable
    public static GLContextInfo create() {

        String vendor = GL11C.glGetString(GL11C.GL_VENDOR);
        String renderer = GL11C.glGetString(GL11C.GL_RENDERER);
        String version = GL11C.glGetString(GL11C.GL_VERSION);

        if (vendor == null || renderer == null || version == null) {
            return null;
        }

        return new GLContextInfo(vendor, renderer, version);
    }
}
