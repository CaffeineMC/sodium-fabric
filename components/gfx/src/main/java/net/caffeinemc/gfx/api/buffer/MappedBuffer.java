package net.caffeinemc.gfx.api.buffer;

import java.nio.ByteBuffer;

/**
 * A buffer object whose underlying storage is exposed to the client as a {@link ByteBuffer}. The client can freely read
 * to and write to the memory represented by this pointer, so long as the buffer is mapped with the appropriate access
 * flags.
 */
public interface MappedBuffer extends Buffer {
    /**
     * Signals that the data represented in the given range should have the changes reflected to the server. If the
     * buffer is mapped with {@link BufferMapFlags#EXPLICIT_FLUSH}, you must call this method after making changes to
     * the mapped memory. If the buffer is not mapped with this flag, then calls to this method will return immediately.
     *
     * @param pos The offset (in bytes) from which to flush
     * @param length The number of bytes to flush
     */
    void flush(long pos, long length);

    /**
     * @return Returns a {@link ByteBuffer} which represents the underlying storage of the buffer, mapped in
     * client address space. The caller must be careful to obey the flags that were used to create the mapped buffer
     * when accessing the underlying storage, otherwise undefined behavior (including crashes) may occur.
     */
    ByteBuffer view();

    /**
     * Helper method for {@link MappedBuffer#flush(long, long)} which flushes the contents of the entire buffer.
     */
    default void flush() {
        this.flush(0, this.capacity());
    }
}
