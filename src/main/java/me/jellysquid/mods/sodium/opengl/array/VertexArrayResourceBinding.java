package me.jellysquid.mods.sodium.opengl.array;

import me.jellysquid.mods.sodium.opengl.attribute.VertexAttributeBinding;

public record VertexArrayResourceBinding<T extends Enum<T>>(T target,
                                                            VertexAttributeBinding[] attributeBindings
) {
}
