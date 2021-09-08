package me.jellysquid.mods.thingl.tessellation;

import me.jellysquid.mods.thingl.array.VertexArrayImpl;
import me.jellysquid.mods.thingl.attribute.VertexAttributeBinding;
import me.jellysquid.mods.thingl.buffer.BufferImpl;
import me.jellysquid.mods.thingl.device.RenderDevice;
import me.jellysquid.mods.thingl.device.RenderDeviceImpl;
import org.lwjgl.opengl.GL20C;
import org.lwjgl.opengl.GL33C;

public class TessellationImpl implements Tessellation {
    private final RenderDevice device;
    private final VertexArrayImpl array;
    private final PrimitiveType primitiveType;

    public TessellationImpl(RenderDeviceImpl device, PrimitiveType primitiveType, TessellationBinding[] bindings) {
        this.primitiveType = primitiveType;

        this.device = device;
        this.array = new VertexArrayImpl(device);

        this.bind();

        for (TessellationBinding binding : bindings) {
            var buffer = (BufferImpl) binding.buffer();
            buffer.bind(binding.target());

            for (VertexAttributeBinding attrib : binding.attributeBindings()) {
                if (attrib.isInteger()) {
                    GL33C.glVertexAttribIPointer(attrib.getIndex(), attrib.getCount(), attrib.getFormat(),
                            attrib.getStride(), attrib.getPointer());
                } else {
                    GL20C.glVertexAttribPointer(attrib.getIndex(), attrib.getCount(), attrib.getFormat(), attrib.isNormalized(),
                            attrib.getStride(), attrib.getPointer());
                }

                GL20C.glEnableVertexAttribArray(attrib.getIndex());
            }
        }
    }

    public PrimitiveType getPrimitiveType() {
        return this.primitiveType;
    }

    public void delete() {
        this.device.deleteVertexArray(this.array);
    }

    public void bind() {
        this.array.bind();
    }

}
