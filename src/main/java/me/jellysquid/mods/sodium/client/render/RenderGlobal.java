package me.jellysquid.mods.sodium.client.render;

import org.lwjgl.system.MemoryStack;

/**
 * WARNING: This class should never be accessed by any thread other than the main render thread.
 */
public class RenderGlobal {
    /**
     * A memory stack which can be used for vertex assembly and copying.
     */
    public static final MemoryStack VERTEX_DATA = MemoryStack.create(1024 * 64);
}
