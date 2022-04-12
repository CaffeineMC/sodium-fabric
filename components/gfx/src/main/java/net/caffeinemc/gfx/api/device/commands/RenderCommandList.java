package net.caffeinemc.gfx.api.device.commands;

import net.caffeinemc.gfx.api.buffer.Buffer;
import net.caffeinemc.gfx.api.types.ElementFormat;
import net.caffeinemc.gfx.api.types.PrimitiveType;

public interface RenderCommandList<T extends Enum<T>> {
    void bindElementBuffer(Buffer buffer);

    void bindVertexBuffer(T target, Buffer buffer, int offset, int stride);

    void bindCommandBuffer(Buffer buffer);

    void multiDrawElementsIndirect(PrimitiveType primitiveType, ElementFormat elementType, int indirectOffset, int indirectCount);
}
