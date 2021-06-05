package me.jellysquid.mods.sodium.client.gl.tessellation;

import me.jellysquid.mods.sodium.client.gl.attribute.GlVertexAttribute;
import me.jellysquid.mods.sodium.client.gl.attribute.GlVertexAttributeBinding;
import me.jellysquid.mods.sodium.client.gl.attribute.GlVertexFormat;
import me.jellysquid.mods.sodium.client.gl.buffer.GlBuffer;

public class TessellationBinding {
    private final GlBuffer buffer;
    private final GlVertexAttributeBinding[] bindings;
    private final boolean instanced;

    public TessellationBinding(GlBuffer buffer, GlVertexAttributeBinding[] bindings, boolean instanced) {
        this.buffer = buffer;
        this.bindings = bindings;
        this.instanced = instanced;
    }

    public GlBuffer getBuffer() {
        return this.buffer;
    }

    public GlVertexAttributeBinding[] getAttributeBindings() {
        return this.bindings;
    }

    public boolean isInstanced() {
        return this.instanced;
    }
}
