package me.jellysquid.mods.sodium.client.gl.array;

import me.jellysquid.mods.sodium.client.gl.GlObject;
import org.lwjgl.opengl.GL45C;

import java.util.List;
import java.util.Map;

public class VertexArray<T extends Enum<T>> extends GlObject {
    private final VertexArrayDescription<T> desc;

    public VertexArray(int id, VertexArrayDescription<T> desc) {
        this.setHandle(id);

        this.setVertexBindings(desc.vertexBindings());

        this.desc = desc;
    }

    private void setVertexBindings(List<VertexBufferBinding<T>> bindings) {
        var handle = this.handle();

        for (var bufferIndex = 0; bufferIndex < bindings.size(); bufferIndex++) {
            var bufferBinding = bindings.get(bufferIndex);

            for (var attrib : bufferBinding.attributeBindings()) {
                GL45C.glVertexArrayAttribBinding(handle, attrib.getIndex(), bufferIndex);

                if (attrib.isIntType()) {
                    GL45C.glVertexArrayAttribIFormat(handle, attrib.getIndex(),
                            attrib.getCount(), attrib.getFormat(), attrib.getOffset());
                } else {
                    GL45C.glVertexArrayAttribFormat(handle, attrib.getIndex(),
                            attrib.getCount(), attrib.getFormat(), attrib.isNormalized(), attrib.getOffset());
                }

                GL45C.glEnableVertexArrayAttrib(handle, attrib.getIndex());
            }
        }
    }

    public VertexArrayBindings<T> createBindings(Map<T, VertexArrayBuffer> bindings) {
        return new VertexArrayBindings<>(this.desc.type(), bindings);
    }
}
