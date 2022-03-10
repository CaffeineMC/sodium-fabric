package net.caffeinemc.gfx.api.array;

import java.util.List;

public record VertexArrayDescription<T extends Enum<T>>(
        T[] targets,
        List<VertexArrayResourceBinding<T>> vertexBindings) {
}
