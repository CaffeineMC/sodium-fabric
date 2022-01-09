package me.jellysquid.mods.sodium.opengl.array;

public interface VertexArray<T extends Enum<T>> {
    T[] getBufferTargets();

    int handle();
}
