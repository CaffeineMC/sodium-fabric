package net.caffeinemc.gfx.api.array;

import java.util.List;

/**
 * Describes the organization of a vertex array.
 *
 * @param targets The generic buffer names that generic vertex attributes can source from
 * @param <T> The enumeration over the generic buffer names
 */
public record VertexArrayDescription<T extends Enum<T>>(
        T[] targets,
        List<VertexArrayResourceBinding<T>> vertexBindings) {
}
