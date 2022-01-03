package me.jellysquid.mods.sodium.opengl.array;

import me.jellysquid.mods.sodium.opengl.attribute.GlVertexAttributeBinding;

public record VertexBufferBinding<T extends Enum<T>>(T target,
                                                     GlVertexAttributeBinding[] attributeBindings) {
}
