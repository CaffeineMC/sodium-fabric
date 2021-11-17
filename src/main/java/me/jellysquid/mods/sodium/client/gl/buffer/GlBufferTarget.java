package me.jellysquid.mods.sodium.client.gl.buffer;

import org.lwjgl.opengl.GL20C;
import org.lwjgl.opengl.GL31C;
import org.lwjgl.opengl.GL43C;

public enum GlBufferTarget {
    ARRAY_BUFFER(GL20C.GL_ARRAY_BUFFER, GL20C.GL_ARRAY_BUFFER_BINDING),
    ELEMENT_BUFFER(GL20C.GL_ELEMENT_ARRAY_BUFFER, GL20C.GL_ELEMENT_ARRAY_BUFFER_BINDING),
    COPY_READ_BUFFER(GL31C.GL_COPY_READ_BUFFER, GL31C.GL_COPY_READ_BUFFER),
    COPY_WRITE_BUFFER(GL31C.GL_COPY_WRITE_BUFFER, GL31C.GL_COPY_WRITE_BUFFER),
    SHADER_STORAGE_BUFFER(GL43C.GL_SHADER_STORAGE_BUFFER, GL43C.GL_SHADER_STORAGE_BUFFER_BINDING);

    public static final GlBufferTarget[] VALUES = GlBufferTarget.values();
    public static final int COUNT = VALUES.length;

    private final int target;
    private final int binding;

    GlBufferTarget(int target, int binding) {
        this.target = target;
        this.binding = binding;
    }

    public int getTargetParameter() {
        return this.target;
    }

    public int getBindingParameter() {
        return this.binding;
    }
}
