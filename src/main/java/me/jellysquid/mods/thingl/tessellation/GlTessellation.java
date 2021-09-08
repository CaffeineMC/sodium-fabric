package me.jellysquid.mods.thingl.tessellation;

import me.jellysquid.mods.thingl.array.GlVertexArray;
import me.jellysquid.mods.thingl.attribute.GlVertexAttributeBinding;
import me.jellysquid.mods.thingl.device.RenderDevice;
import me.jellysquid.mods.thingl.device.RenderDeviceImpl;
import org.lwjgl.opengl.GL20C;
import org.lwjgl.opengl.GL30C;
import org.lwjgl.opengl.GL33C;

public class GlTessellation {
    private final RenderDevice device;
    private final GlVertexArray array;
    private final GlPrimitiveType primitiveType;

    public GlTessellation(RenderDeviceImpl device, GlPrimitiveType primitiveType, TessellationBinding[] bindings) {
        this.primitiveType = primitiveType;

        this.device = device;
        this.array = new GlVertexArray(device);

        this.bind();

        for (TessellationBinding binding : bindings) {
            var buffer = binding.buffer();
            buffer.bind(binding.target());

            for (GlVertexAttributeBinding attrib : binding.attributeBindings()) {
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

    public GlPrimitiveType getPrimitiveType() {
        return this.primitiveType;
    }

    public void delete() {
        this.device.deleteVertexArray(this.array);
    }

    public void bind() {
        this.array.bind();
    }

}
