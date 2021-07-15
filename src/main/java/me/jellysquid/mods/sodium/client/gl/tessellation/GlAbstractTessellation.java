package me.jellysquid.mods.sodium.client.gl.tessellation;

import me.jellysquid.mods.sodium.client.gl.attribute.GlVertexAttributeBinding;
import me.jellysquid.mods.sodium.client.gl.buffer.GlBuffer;
import me.jellysquid.mods.sodium.client.gl.buffer.GlBufferTarget;
import me.jellysquid.mods.sodium.client.gl.device.CommandList;
import org.lwjgl.opengl.GL20C;

public abstract class GlAbstractTessellation implements GlTessellation {
    protected final GlPrimitiveType primitiveType;
    protected final TessellationBinding[] bindings;
    protected final GlBuffer indexBuffer;

    protected GlAbstractTessellation(GlPrimitiveType primitiveType, TessellationBinding[] bindings, GlBuffer indexBuffer) {
        this.primitiveType = primitiveType;
        this.bindings = bindings;
        this.indexBuffer = indexBuffer;
    }

    @Override
    public GlPrimitiveType getPrimitiveType() {
        return this.primitiveType;
    }

    protected void bindAttributes(CommandList commandList) {
        for (TessellationBinding binding : this.bindings) {
            commandList.bindBuffer(GlBufferTarget.ARRAY_BUFFER, binding.buffer());

            for (GlVertexAttributeBinding attrib : binding.attributeBindings()) {
                GL20C.glVertexAttribPointer(attrib.getIndex(), attrib.getCount(), attrib.getFormat(), attrib.isNormalized(),
                        attrib.getStride(), attrib.getPointer());
                GL20C.glEnableVertexAttribArray(attrib.getIndex());
            }
        }

        if (this.indexBuffer != null) {
            commandList.bindBuffer(GlBufferTarget.ELEMENT_BUFFER, this.indexBuffer);
        }
    }
}
