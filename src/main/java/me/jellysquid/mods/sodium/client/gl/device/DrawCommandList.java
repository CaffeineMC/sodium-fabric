package me.jellysquid.mods.sodium.client.gl.device;

import me.jellysquid.mods.sodium.client.gl.tessellation.GlIndexType;
import me.jellysquid.mods.sodium.client.gl.tessellation.GlPrimitiveType;
import org.lwjgl.PointerBuffer;

import java.nio.IntBuffer;

public interface DrawCommandList extends AutoCloseable {
    void multiDrawElementsBaseVertex(PointerBuffer pointer, IntBuffer count, IntBuffer baseVertex, GlIndexType indexType, GlPrimitiveType primitiveType);
    void drawElements(GlPrimitiveType primitiveType, GlIndexType indexType, long pointer, int count);
    void drawElementsBaseVertex(GlPrimitiveType primitiveType, GlIndexType elementType, long basePointer, int baseVertex, int elementCount);

    void endTessellating();

    void flush();

    @Override
    default void close() {
        this.flush();
    }
}
