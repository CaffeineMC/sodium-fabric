package me.jellysquid.mods.thingl.array;

import me.jellysquid.mods.thingl.GlObject;
import org.lwjgl.opengl.GL30C;

/**
 * Provides Vertex Array functionality on supported platforms.
 */
public class GlVertexArray extends GlObject {
    public GlVertexArray() {
        this.setHandle(GL30C.glGenVertexArrays());
    }
}
