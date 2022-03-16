package net.caffeinemc.gfx.api.array;

import net.caffeinemc.gfx.api.array.attribute.VertexAttributeBinding;

/**
 * Describes the vertex attributes which will be sourced for the given generic buffer name in the vertex array. During
 * rendering, the actual buffer objects containing the data will be bound to the generic names in the vertex array,
 * which the provided vertex attributes in this description will then source from.
 *
 * @param <T> The enumeration over the generic buffer names for a given vertex array
 */
public record VertexArrayResourceBinding<T extends Enum<T>>(T target, VertexAttributeBinding[] attributeBindings) {
}
