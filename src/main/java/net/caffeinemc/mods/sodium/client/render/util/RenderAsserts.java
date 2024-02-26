package net.caffeinemc.mods.sodium.client.render.util;

import com.mojang.blaze3d.systems.RenderSystem;

public class RenderAsserts {
    /**
     * Checks that the thread calling this function is the main render thread. This is useful for ensuring that OpenGL
     * APIs are not accessed from off-thread incorrectly, which is known to cause severe issues.
     *
     * @throws IllegalStateException If the current thread is not the main render thread
     * @return Always true, since an exception is thrown otherwise
     */
    public static boolean validateCurrentThread() {
        if (!RenderSystem.isOnRenderThread()) {
            throw new IllegalStateException("Accessing OpenGL functions from outside the main render thread is not supported when using Sodium");
        }

        return true;
    }
}
