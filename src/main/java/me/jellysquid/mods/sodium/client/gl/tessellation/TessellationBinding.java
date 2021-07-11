package me.jellysquid.mods.sodium.client.gl.tessellation;

import me.jellysquid.mods.sodium.client.gl.attribute.GlVertexAttributeBinding;
import me.jellysquid.mods.sodium.client.gl.buffer.GlBuffer;

public record TessellationBinding(GlBuffer buffer,
                                  GlVertexAttributeBinding[] attributeBindings) {
}
