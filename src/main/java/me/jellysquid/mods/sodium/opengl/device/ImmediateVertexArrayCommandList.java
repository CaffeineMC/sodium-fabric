package me.jellysquid.mods.sodium.opengl.device;

import me.jellysquid.mods.sodium.opengl.buffer.GlBuffer;
import me.jellysquid.mods.sodium.opengl.array.*;
import org.lwjgl.PointerBuffer;
import org.lwjgl.opengl.GL32C;
import org.lwjgl.opengl.GL45C;

import java.nio.IntBuffer;

public class ImmediateVertexArrayCommandList<T extends Enum<T>> implements VertexArrayCommandList<T> {
    private final VertexArray<T> array;

    public ImmediateVertexArrayCommandList(VertexArray<T> array) {
        this.array = array;
    }

    @Override
    public void bindVertexBuffers(VertexArrayBindings<T> bindings) {
        var slots = bindings.slots;

        for (int i = 0; i < slots.length; i++) {
            var slot = slots[i];

            var buffer = slot.buffer();
            var stride = slot.stride();

            GL45C.glVertexArrayVertexBuffer(this.array.handle(), i, buffer.handle(), 0, stride);
        }
    }

    @Override
    public void bindElementBuffer(GlBuffer buffer) {
        GL45C.glVertexArrayElementBuffer(this.array.handle(), buffer.handle());
    }

    @Override
    public void multiDrawElementsBaseVertex(PointerBuffer pointer, IntBuffer count, IntBuffer baseVertex, GlIndexType indexType, GlPrimitiveType primitiveType) {
        GL32C.glMultiDrawElementsBaseVertex(primitiveType.getId(), count, indexType.getFormatId(), pointer, baseVertex);
    }

    @Override
    public void drawElementsBaseVertex(GlPrimitiveType primitiveType, GlIndexType elementType, long elementPointer, int baseVertex, int elementCount) {
        GL32C.glDrawElementsBaseVertex(primitiveType.getId(), elementCount, elementType.getFormatId(), elementPointer, baseVertex);
    }
}
