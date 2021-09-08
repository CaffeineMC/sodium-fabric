package me.jellysquid.mods.thingl.tessellation;

import me.jellysquid.mods.thingl.array.VertexArrayImpl;
import me.jellysquid.mods.thingl.attribute.VertexAttributeBinding;
import me.jellysquid.mods.thingl.buffer.BufferImpl;
import me.jellysquid.mods.thingl.buffer.BufferTarget;
import me.jellysquid.mods.thingl.device.RenderDevice;
import me.jellysquid.mods.thingl.device.RenderDeviceImpl;
import me.jellysquid.mods.thingl.tessellation.binding.ElementBufferBinding;
import me.jellysquid.mods.thingl.tessellation.binding.VertexBufferBinding;
import org.lwjgl.opengl.GL20C;
import org.lwjgl.opengl.GL33C;

public abstract class TessellationImpl implements Tessellation {
    private final RenderDevice device;
    private final VertexArrayImpl array;
    private final PrimitiveType primitiveType;

    public TessellationImpl(RenderDeviceImpl device, PrimitiveType primitiveType, VertexBufferBinding[] vertexBindings, ElementBufferBinding elementBinding) {
        this.primitiveType = primitiveType;
        this.array = this.init(device, vertexBindings, elementBinding);
        this.device = device;
    }

    protected abstract VertexArrayImpl init(RenderDeviceImpl device, VertexBufferBinding[] bindings, ElementBufferBinding elementBinding);

    public PrimitiveType getPrimitiveType() {
        return this.primitiveType;
    }

    public void delete() {
        this.device.deleteVertexArray(this.array);
    }

    public void bind() {
        this.array.bind();
    }

    public static class BindlessTessellationImpl extends TessellationImpl {
        public BindlessTessellationImpl(RenderDeviceImpl device, PrimitiveType primitiveType, VertexBufferBinding[] vertexBindings, ElementBufferBinding elementBinding) {
            super(device, primitiveType, vertexBindings, elementBinding);
        }

        @Override
        protected VertexArrayImpl init(RenderDeviceImpl device, VertexBufferBinding[] bindings, ElementBufferBinding elementBinding) {
            var vao = VertexArrayImpl.create(device, true);
            var dsa = device.getDeviceFunctions()
                    .getDirectStateAccessFunctions();

            for (int vertexBufferIndex = 0; vertexBufferIndex < bindings.length; vertexBufferIndex++) {
                VertexBufferBinding binding = bindings[vertexBufferIndex];
                BufferImpl buffer = (BufferImpl) binding.getBuffer();

                dsa.vertexArrayVertexBuffer(vao.handle(), vertexBufferIndex, buffer.handle(), 0, binding.getStride());

                for (VertexAttributeBinding attribBinding : binding.getAttributeBindings()) {
                    dsa.enableVertexArrayAttrib(vao.handle(), attribBinding.getIndex());
                    dsa.vertexArrayAttribBinding(vao.handle(), attribBinding.getIndex(), vertexBufferIndex);

                    var attrib = attribBinding.getAttribute();

                    if (attribBinding.isInteger()) {
                        dsa.vertexArrayAttribIFormat(vao.handle(), attribBinding.getIndex(),
                                attrib.getCount(), attrib.getFormat(), attrib.getPointer());

                    } else {
                        dsa.vertexArrayAttribFormat(vao.handle(), attribBinding.getIndex(),
                                attrib.getCount(), attrib.getFormat(), attrib.isNormalized(), attrib.getPointer());
                    }
                }
            }

            var elementBuffer = (BufferImpl) elementBinding.buffer();
            dsa.vertexArrayElementBuffer(vao.handle(), elementBuffer.handle());

            return vao;
        }
    }

    public static class FallbackTessellationImpl extends TessellationImpl {
        public FallbackTessellationImpl(RenderDeviceImpl device, PrimitiveType primitiveType, VertexBufferBinding[] vertexBindings, ElementBufferBinding elementBinding) {
            super(device, primitiveType, vertexBindings, elementBinding);
        }

        @Override
        protected VertexArrayImpl init(RenderDeviceImpl device, VertexBufferBinding[] vertexBindings, ElementBufferBinding elementBinding) {
            var vao = VertexArrayImpl.create(device, false);
            vao.bind();

            for (VertexBufferBinding binding : vertexBindings) {
                var buffer = (BufferImpl) binding.getBuffer();
                buffer.bind(BufferTarget.ARRAY_BUFFER);

                int stride = binding.getStride();

                for (VertexAttributeBinding attribBinding : binding.getAttributeBindings()) {
                    var attrib = attribBinding.getAttribute();

                    if (attribBinding.isInteger()) {
                        GL33C.glVertexAttribIPointer(attribBinding.getIndex(), attrib.getCount(), attrib.getFormat(),
                                stride, attrib.getPointer());
                    } else {
                        GL20C.glVertexAttribPointer(attribBinding.getIndex(), attrib.getCount(), attrib.getFormat(), attrib.isNormalized(),
                                stride, attrib.getPointer());
                    }

                    GL20C.glEnableVertexAttribArray(attribBinding.getIndex());
                }
            }

            var elementBuffer = (BufferImpl) elementBinding.buffer();
            elementBuffer.bind(BufferTarget.ELEMENT_BUFFER);

            return vao;
        }
    }
}
