package net.caffeinemc.gfx.api.device.commands;

import net.caffeinemc.gfx.api.buffer.Buffer;
import net.caffeinemc.gfx.api.types.ElementFormat;
import net.caffeinemc.gfx.api.types.PrimitiveType;

public interface RenderCommandList<T extends Enum<T>> {
    // TODO: try to incorporate binds in actual rendering commands if possible
    void bindElementBuffer(Buffer buffer);

    void bindVertexBuffer(T target, Buffer buffer, int offset, int stride);

    void bindCommandBuffer(Buffer buffer);

    void multiDrawElementsIndirect(PrimitiveType primitiveType, ElementFormat elementType, long indirectOffset, int indirectCount, int stride);

    // REQUIRES 4.6 OR ARB_indirect_parameters
    // TODO: separate these into a separate class and bar them with glcaps?

    void bindParameterBuffer(Buffer buffer);

    void multiDrawElementsIndirectCount(PrimitiveType primitiveType, ElementFormat elementType, long indirectOffset, long countOffset, int maxCount, int stride);
}
