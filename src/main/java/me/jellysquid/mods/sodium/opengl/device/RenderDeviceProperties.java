package me.jellysquid.mods.sodium.opengl.device;

import org.lwjgl.opengl.GL45C;

public class RenderDeviceProperties {
    public final int uniformBufferOffsetAlignment = GL45C.glGetInteger(GL45C.GL_UNIFORM_BUFFER_OFFSET_ALIGNMENT);
}
