package me.jellysquid.mods.sodium.opengl.array;

import me.jellysquid.mods.sodium.opengl.buffer.GlBuffer;
import org.lwjgl.PointerBuffer;

import java.nio.IntBuffer;

public interface VertexArrayCommandList<T extends Enum<T>> {
    void bindVertexBuffers(VertexArrayBindings<T> bindings);

    void bindElementBuffer(GlBuffer buffer);

    void multiDrawElementsBaseVertex(PointerBuffer pointer, IntBuffer count, IntBuffer baseVertex, GlIndexType indexType, GlPrimitiveType primitiveType);

    void drawElementsBaseVertex(GlPrimitiveType primitiveType, GlIndexType elementType, long elementPointer, int baseVertex, int elementCount);
}
