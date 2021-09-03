package me.jellysquid.mods.thingl.tessellation;

import me.jellysquid.mods.thingl.attribute.GlVertexAttributeBinding;
import me.jellysquid.mods.thingl.buffer.GlBuffer;
import me.jellysquid.mods.thingl.buffer.GlBufferTarget;

public record TessellationBinding(GlBufferTarget target,
                                  GlBuffer buffer,
                                  GlVertexAttributeBinding[] attributeBindings) {
    public static TessellationBinding forVertexBuffer(GlBuffer buffer, GlVertexAttributeBinding[] attributes) {
        return new TessellationBinding(GlBufferTarget.ARRAY_BUFFER, buffer, attributes);
    }

    public static TessellationBinding forElementBuffer(GlBuffer buffer) {
        return new TessellationBinding(GlBufferTarget.ELEMENT_BUFFER, buffer, new GlVertexAttributeBinding[0]);
    }
}
