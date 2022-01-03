package me.jellysquid.mods.sodium.opengl.array;

import java.util.List;

public record VertexArrayDescription<T extends Enum<T>>(
        Class<T> type,
        List<VertexBufferBinding<T>> vertexBindings) {
}
