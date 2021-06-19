package me.jellysquid.mods.sodium.client.gl.device;

import org.lwjgl.PointerBuffer;

import java.nio.IntBuffer;

public interface DrawCommandList extends AutoCloseable {
    void multiDrawElementsIndirect(long pointer, int count, int stride);

    void multiDrawElementsBaseVertex(PointerBuffer pointer, IntBuffer count, IntBuffer baseVertex);

    void endTessellating();

    void flush();

    @Override
    default void close() {
        this.flush();
    }
}
