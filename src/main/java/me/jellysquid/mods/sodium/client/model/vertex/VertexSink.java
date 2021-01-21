package me.jellysquid.mods.sodium.client.model.vertex;

/**
 * Vertex sinks allow vertex data to be quickly written out to a {@link VertexDrain} while providing
 * compile-time data format contracts. Generally, you will want a format-specific vertex sink, such as
 * {@link me.jellysquid.mods.sodium.client.model.vertex.formats.quad.QuadVertexSink} in order to write
 * vertex data.
 */
public interface VertexSink {
    /**
     * Ensures the backing storage to this sink has enough space for the given number of vertices to be written. This
     * should be called with the number of vertices you expect to write before you make calls to write vertices.
     *
     * If the caller tries to write vertices without calling this method, or writes more vertices than they ensured
     * there was capacity for, an {@link java.nio.BufferUnderflowException} may occur.
     *
     * When writing batches of vertices (such as those belonging to a primitive or a large model), it is best practice
     * to simply call this method once at the start with the number of vertices you plan to write. This ensures the
     * backing storage will only be resized once (if necessary) to fit the incoming vertex data.
     *
     * @param count The number of vertices
     */
    void ensureCapacity(int count);

    /**
     * Flushes any written vertex data to the {@link VertexDrain} this sink is connected to, ensuring it is actually
     * written to the backing storage. This should be called after vertex data has been written to this sink.
     *
     * It is valid to flush a sink at any time. Only vertices that have been written since the last flush will be
     * flushed when calling this method. If no vertices need to be flushed, this method does nothing.
     *
     * For optimal performance, callers should wait until they have written out as much vertex data as possible before
     * flushing, in effect batching their writes.
     */
    void flush();
}
