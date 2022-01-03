package me.jellysquid.mods.sodium.opengl.device;

import me.jellysquid.mods.sodium.opengl.array.GlIndexType;
import me.jellysquid.mods.sodium.opengl.array.GlPrimitiveType;
import org.lwjgl.PointerBuffer;

import java.nio.IntBuffer;

public interface DrawCommandList extends AutoCloseable {
    void multiDrawElementsBaseVertex(PointerBuffer pointer, IntBuffer count, IntBuffer baseVertex, GlIndexType indexType, GlPrimitiveType primitiveType);
    void drawElements(GlPrimitiveType primitiveType, GlIndexType indexType, long pointer, int count);
    void drawElementsBaseVertex(GlPrimitiveType primitiveType, GlIndexType elementType, long basePointer, int baseVertex, int elementCount);
}
