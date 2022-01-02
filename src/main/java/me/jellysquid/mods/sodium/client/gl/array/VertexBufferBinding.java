package me.jellysquid.mods.sodium.client.gl.array;

import me.jellysquid.mods.sodium.client.gl.attribute.GlVertexAttributeBinding;

public record VertexBufferBinding<T extends Enum<T>>(T target,
                                                     GlVertexAttributeBinding[] attributeBindings) {
}
