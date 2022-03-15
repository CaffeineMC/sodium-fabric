package net.caffeinemc.sodium.render.vertex.type;

import net.caffeinemc.sodium.SodiumClientMod;
import net.caffeinemc.sodium.render.vertex.VertexSink;
import net.caffeinemc.sodium.render.vertex.buffer.VertexBufferView;

public interface BlittableVertexType<T extends VertexSink> extends BufferVertexType<T> {
    /**
     * Creates a {@link VertexSink} which writes into a {@link VertexBufferView}. This allows for specialization
     * when the memory storage is known.
     *
     * @param buffer The backing vertex buffer
     * @param direct True if direct memory access is allowed, otherwise false
     */
    T createBufferWriter(VertexBufferView buffer, boolean direct);

    default T createBufferWriter(VertexBufferView buffer) {
        return this.createBufferWriter(buffer, SodiumClientMod.isDirectMemoryAccessEnabled());
    }
}
