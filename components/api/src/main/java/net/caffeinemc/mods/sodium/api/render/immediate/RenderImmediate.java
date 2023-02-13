package net.caffeinemc.mods.sodium.api.render.immediate;

import org.lwjgl.system.MemoryStack;

/**
 * This class contains some global state needed in immediate-mode rendering.
 * WARNING: This class should never be accessed by any thread other than the main render thread.
 */
public class RenderImmediate {
    /**
     * A memory stack which can be used for vertex assembly and copying.
     */
    public static final MemoryStack VERTEX_DATA = MemoryStack.create(1024 * 64);
}
