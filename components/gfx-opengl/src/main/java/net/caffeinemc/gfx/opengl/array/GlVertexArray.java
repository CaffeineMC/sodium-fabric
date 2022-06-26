package net.caffeinemc.gfx.opengl.array;

import java.util.List;
import net.caffeinemc.gfx.api.array.VertexArray;
import net.caffeinemc.gfx.api.array.VertexArrayDescription;
import net.caffeinemc.gfx.api.array.VertexArrayResourceBinding;
import net.caffeinemc.gfx.opengl.GlEnum;
import net.caffeinemc.gfx.opengl.GlObject;
import org.lwjgl.opengl.GL45C;

public class GlVertexArray<T extends Enum<T>> extends GlObject implements VertexArray<T> {
    private final VertexArrayDescription<T> desc;

    public GlVertexArray(VertexArrayDescription<T> desc) {
        this.setHandle(GL45C.glCreateVertexArrays());

        this.setAttributeBindings(desc.vertexBindings());

        this.desc = desc;
    }

    private void setAttributeBindings(List<VertexArrayResourceBinding<T>> bindings) {
        var handle = this.getHandle();

        for (var bufferIndex = 0; bufferIndex < bindings.size(); bufferIndex++) {
            var bufferBinding = bindings.get(bufferIndex);

            for (var binding : bufferBinding.attributeBindings()) {
                var attrib = binding.attribute();
                var index = binding.index();

                GL45C.glVertexArrayAttribBinding(handle, index, bufferIndex);

                if (attrib.intType()) {
                    GL45C.glVertexArrayAttribIFormat(handle, index,
                            attrib.count(), GlEnum.from(attrib.format()), attrib.offset());
                } else {
                    GL45C.glVertexArrayAttribFormat(handle, index,
                            attrib.count(), GlEnum.from(attrib.format()), attrib.normalized(), attrib.offset());
                }

                GL45C.glEnableVertexArrayAttrib(handle, index);
            }
        }
    }

    @Override
    public T[] getBufferTargets() {
        return this.desc.targets();
    }

    public static int getHandle(VertexArray<?> array) {
        return ((GlVertexArray<?>) array).getHandle();
    }
}
