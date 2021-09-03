package me.jellysquid.mods.thingl.device;

import me.jellysquid.mods.thingl.tessellation.GlIndexType;
import org.lwjgl.PointerBuffer;

import java.nio.IntBuffer;

public interface DrawCommandList extends AutoCloseable {
    void multiDrawElementsBaseVertex(PointerBuffer pointer, IntBuffer count, IntBuffer baseVertex, GlIndexType indexType);

    void endTessellating();

    void flush();

    @Override
    default void close() {
        this.flush();
    }
}
