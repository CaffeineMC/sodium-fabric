package net.caffeinemc.gfx.api.array;

import net.caffeinemc.gfx.api.array.attribute.VertexAttributeBinding;

public record VertexArrayResourceBinding<T extends Enum<T>>(T target,
                                                            VertexAttributeBinding[] attributeBindings
) {
}
