package me.jellysquid.mods.sodium.opengl.array;

import me.jellysquid.mods.sodium.opengl.buffer.Buffer;
import me.jellysquid.mods.sodium.opengl.types.IntType;
import me.jellysquid.mods.sodium.opengl.types.PrimitiveType;
import org.lwjgl.PointerBuffer;

import java.nio.IntBuffer;

public interface DrawCommandList<T extends Enum<T>> {
    void bindElementBuffer(Buffer buffer);

    void multiDrawElementsBaseVertex(PointerBuffer pointer, IntBuffer count, IntBuffer baseVertex, IntType indexType, PrimitiveType primitiveType);

    void drawElementsBaseVertex(PrimitiveType primitiveType, IntType elementType, long elementPointer, int baseVertex, int elementCount);

    void drawElements(PrimitiveType primitiveType, IntType elementType, long elementPointer, int elementCount);

    void bindVertexBuffer(T target, Buffer buffer, int offset, int stride);

    void multiDrawElementsIndirect(Buffer indirectBuffer, int indirectOffset, int indirectCount, IntType elementType, PrimitiveType primitiveType);
}
