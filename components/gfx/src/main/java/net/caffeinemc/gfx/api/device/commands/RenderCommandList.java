package net.caffeinemc.gfx.api.device.commands;

import net.caffeinemc.gfx.api.buffer.Buffer;
import net.caffeinemc.gfx.api.types.ElementFormat;
import net.caffeinemc.gfx.api.types.PrimitiveType;
import org.lwjgl.PointerBuffer;

import java.nio.IntBuffer;

public interface RenderCommandList<T extends Enum<T>> {
    void bindElementBuffer(Buffer buffer);

    void multiDrawElementsBaseVertex(PointerBuffer pointer, IntBuffer count, IntBuffer baseVertex, ElementFormat indexType, PrimitiveType primitiveType);

    void drawElementsBaseVertex(PrimitiveType primitiveType, ElementFormat elementType, long elementPointer, int baseVertex, int elementCount);

    void drawElements(PrimitiveType primitiveType, ElementFormat elementType, long elementPointer, int elementCount);

    void bindVertexBuffer(T target, Buffer buffer, int offset, int stride);

    void multiDrawElementsIndirect(Buffer indirectBuffer, int indirectOffset, int indirectCount, ElementFormat elementType, PrimitiveType primitiveType);
}
