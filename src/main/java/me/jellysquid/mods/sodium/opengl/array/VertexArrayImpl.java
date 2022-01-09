package me.jellysquid.mods.sodium.opengl.array;

import me.jellysquid.mods.sodium.opengl.ManagedObject;
import org.lwjgl.opengl.GL45C;

import java.util.List;

public class VertexArrayImpl<T extends Enum<T>> extends ManagedObject implements VertexArray<T> {
    private final VertexArrayDescription<T> desc;

    public VertexArrayImpl(int id, VertexArrayDescription<T> desc) {
        this.setHandle(id);

        this.setVertexBindings(desc.vertexBindings());

        this.desc = desc;
    }

    private void setVertexBindings(List<VertexArrayResourceBinding<T>> bindings) {
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

    @Override
    public T[] getBufferTargets() {
        return this.desc.targets();
    }
}
