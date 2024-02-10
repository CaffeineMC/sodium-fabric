package net.caffeinemc.mods.sodium.client.compatibility.environment;

import org.jetbrains.annotations.Nullable;
import org.lwjgl.opengl.GL11C;

/**
 * Information about an OpenGL graphics context.
 */
public record GLContextInfo(
        /* The vendor which provides the OpenGL implementation. (GL_VENDOR) */
        String vendor,
        /* The name of the renderer used by OpenGL the implementation. (GL_RENDERER) */
        String renderer,
        /* The version of the OpenGL implementation. (GL_VERSION) */
        String version
) {
    /**
     * Obtains information about the current OpenGL context. This can be used to identify known implementations and
     * apply workarounds for them. However, the identifying strings returned may consist of any information whatsoever,
     * and they can not be reliably parsed.
     *
     * <p><b>NOTE:</b> This function must only be called on a thread with a current OpenGL context!</p>
     *
     * @return The identifying strings of the context, or null if the information is not available
     */
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
