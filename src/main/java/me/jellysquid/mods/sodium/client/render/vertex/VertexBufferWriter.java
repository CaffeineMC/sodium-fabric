package me.jellysquid.mods.sodium.client.render.vertex;

import net.minecraft.client.render.VertexConsumer;
import org.lwjgl.system.MemoryStack;

public interface VertexBufferWriter {
    // TODO: Move this elsewhere?
    MemoryStack STACK = MemoryStack.create(1024 * 64);

    static VertexBufferWriter of(VertexConsumer consumer) {
        return (VertexBufferWriter) consumer;
    }

    /**
     * Writes a range of vertices from a buffer in a given format into this vertex writer.
     *
     * @param ptr    The pointer to read vertices from
     * @param count  The number of vertices to copy
     * @param format The format description of the vertices
     */
    void push(long ptr, int count, VertexFormatDescription format);
}
