package me.jellysquid.mods.sodium.opengl.array;

import java.util.Map;

public interface VertexArray<T extends Enum<T>> {

    VertexArrayResourceSet<T> createResourceSet(Map<T, VertexArrayBuffer> bindings);
}
