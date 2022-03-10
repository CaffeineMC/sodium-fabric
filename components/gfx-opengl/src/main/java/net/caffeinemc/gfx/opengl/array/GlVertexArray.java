package net.caffeinemc.gfx.opengl.array;

import net.caffeinemc.gfx.opengl.GlObject;
import net.caffeinemc.gfx.opengl.GlEnum;
import net.caffeinemc.gfx.api.array.VertexArray;
import net.caffeinemc.gfx.api.array.VertexArrayDescription;
import net.caffeinemc.gfx.api.array.VertexArrayResourceBinding;
import org.lwjgl.opengl.GL45C;

import java.util.List;

public class GlVertexArray<T extends Enum<T>> extends GlObject implements VertexArray<T> {
    private final VertexArrayDescription<T> desc;

    public GlVertexArray(int id, VertexArrayDescription<T> desc) {
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
                            attrib.getCount(), GlEnum.from(attrib.getFormat()), attrib.getOffset());
                } else {
                    GL45C.glVertexArrayAttribFormat(handle, attrib.getIndex(),
                            attrib.getCount(), GlEnum.from(attrib.getFormat()), attrib.isNormalized(), attrib.getOffset());
                }

                GL45C.glEnableVertexArrayAttrib(handle, attrib.getIndex());
            }
        }
    }

    @Override
    public T[] getBufferTargets() {
        return this.desc.targets();
    }

    public static int getHandle(VertexArray<?> array) {
        return ((GlVertexArray<?>) array).handle();
    }
}
