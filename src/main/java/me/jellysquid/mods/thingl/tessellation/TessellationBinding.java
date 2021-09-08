package me.jellysquid.mods.thingl.tessellation;

import me.jellysquid.mods.thingl.attribute.VertexAttributeBinding;
import me.jellysquid.mods.thingl.buffer.Buffer;
import me.jellysquid.mods.thingl.buffer.BufferTarget;

public record TessellationBinding(BufferTarget target,
                                  Buffer buffer,
                                  VertexAttributeBinding[] attributeBindings) {
    public static TessellationBinding forVertexBuffer(Buffer buffer, VertexAttributeBinding[] attributes) {
        return new TessellationBinding(BufferTarget.ARRAY_BUFFER, buffer, attributes);
    }

    public static TessellationBinding forElementBuffer(Buffer buffer) {
        return new TessellationBinding(BufferTarget.ELEMENT_BUFFER, buffer, new VertexAttributeBinding[0]);
    }
}
