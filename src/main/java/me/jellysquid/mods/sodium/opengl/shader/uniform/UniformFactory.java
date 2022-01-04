package me.jellysquid.mods.sodium.opengl.shader.uniform;

public interface UniformFactory<U> {
    U create(int program, int index);
}
