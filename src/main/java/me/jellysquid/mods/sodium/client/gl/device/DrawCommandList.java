package me.jellysquid.mods.sodium.client.gl.device;

import org.lwjgl.PointerBuffer;

import java.nio.IntBuffer;

public interface DrawCommandList extends AutoCloseable {
    void multiDrawElementsBaseVertex(PointerBuffer pointer, IntBuffer count, IntBuffer baseVertex);

    void multiDrawElements(PointerBuffer pointer, IntBuffer count);

    void endTessellating();

    void flush();

    @Override
    default void close() {
        this.flush();
    }
}
