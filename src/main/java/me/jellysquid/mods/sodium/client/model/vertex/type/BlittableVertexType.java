package me.jellysquid.mods.sodium.client.model.vertex.type;

import me.jellysquid.mods.sodium.client.model.vertex.VertexSink;
import me.jellysquid.mods.sodium.client.model.vertex.buffer.VertexBufferView;
import me.jellysquid.mods.sodium.client.render.chunk.format.MaterialIdHolder;

public interface BlittableVertexType<T extends VertexSink> extends BufferVertexType<T> {
    /**
     * Creates a {@link VertexSink} which writes into a {@link VertexBufferView}. This allows for specialization
     * when the memory storage is known.
     *
     * @param buffer The backing vertex buffer
     * @param direct True if direct memory access is allowed, otherwise false
     */
    T createBufferWriter(VertexBufferView buffer, boolean direct);

    default T createBufferWriter(VertexBufferView buffer, boolean direct, MaterialIdHolder idHolder) {
        return createBufferWriter(buffer, direct);
    }
}
