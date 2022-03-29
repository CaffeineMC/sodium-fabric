package net.caffeinemc.gfx.api.buffer;

import java.nio.ByteBuffer;

/**
 * A buffer object that has been allocated but is not yet able to be used by other rendering commands. The buffer's
 * backing memory is mapped into address space, but the location of the memory is undefined.
 *
 * This can be used to create a buffer from a mapped address range *without* requiring a {@link MappedBuffer} to be
 * created, which can reduce some overhead caused by persistent memory mappings.
 */
public interface AllocatedBuffer {
    /**
     * @return The capacity (in bytes) of the buffer.
     */
    long capacity();

    /**
     * @return Returns a {@link ByteBuffer} which represents the underlying storage of the buffer, mapped in
     * client address space.
     */
    ByteBuffer view();
}
