package me.jellysquid.mods.sodium.util.draw;

import me.jellysquid.mods.sodium.SodiumClientMod;
import org.lwjgl.PointerBuffer;

import java.nio.IntBuffer;

/**
 * Provides a fixed-size queue for building a draw-command list usable with
 * {@link org.lwjgl.opengl.GL33C#glMultiDrawElementsBaseVertex(int, IntBuffer, int, PointerBuffer, IntBuffer)}.
 */
public interface MultiDrawBatch {
    static MultiDrawBatch create(int capacity) {
        return SodiumClientMod.isDirectMemoryAccessEnabled() ? new DirectMultiDrawBatch(capacity) : new NioMultiDrawBatch(capacity);
    }

    PointerBuffer getPointerBuffer();

    IntBuffer getCountBuffer();

    IntBuffer getBaseVertexBuffer();

    void begin();

    void add(long pointer, int count, int baseVertex);

    void end();

    void delete();

    boolean isEmpty();

}
