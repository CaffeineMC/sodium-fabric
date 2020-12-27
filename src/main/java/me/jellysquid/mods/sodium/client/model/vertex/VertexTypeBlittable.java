package me.jellysquid.mods.sodium.client.model.vertex;

import me.jellysquid.mods.sodium.client.model.vertex.buffer.VertexBufferView;
import net.minecraft.client.render.VertexFormat;

/**
 * A blittable {@link VertexType} which supports direct copying into buffer memory provided the buffer's vertex format
 * matches that required by the {@link VertexSink}.
 *
 * @param <T> The {@link VertexSink} type this factory produces
 */
public interface VertexTypeBlittable<T extends VertexSink> {
    /**
     * Creates a {@link VertexSink} which writes into a {@link VertexBufferView}. This allows for specialization
     * when the memory storage is known.
     *
     * @param buffer The backing vertex buffer
     * @param direct True if direct memory access is allowed, otherwise false
     */
    T createBufferWriter(VertexBufferView buffer, boolean direct);

    /**
     * @return The {@link VertexFormat} required for blitting (direct writing into buffers)
     */
    VertexFormat getBufferVertexFormat();
}
