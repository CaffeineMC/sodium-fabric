package net.caffeinemc.gfx.api.device.commands;

import net.caffeinemc.gfx.api.buffer.Buffer;
import net.caffeinemc.gfx.api.types.ElementFormat;
import net.caffeinemc.gfx.api.types.PrimitiveType;

public interface RenderCommandList<T extends Enum<T>> {
    // TODO: try to incorporate binds in actual rendering commands if possible
    void bindElementBuffer(Buffer buffer);

    void bindVertexBuffer(T target, Buffer buffer, int offset, int stride);

    void bindCommandBuffer(Buffer buffer);
    
    void multiDrawElementsBaseVertex(PrimitiveType primitiveType, ElementFormat elementType, int drawCount, long indexCountsPtr, long indexOffsetsPtr, long baseVerticesPtr);

    void multiDrawElementsIndirect(PrimitiveType primitiveType, ElementFormat elementType, long indirectOffset, int indirectCount, int stride);

    // REQUIRES 4.6 OR ARB_indirect_parameters

    void bindParameterBuffer(Buffer buffer);

    void multiDrawElementsIndirectCount(PrimitiveType primitiveType, ElementFormat elementType, long indirectOffset, long countOffset, int maxCount, int stride);
}
