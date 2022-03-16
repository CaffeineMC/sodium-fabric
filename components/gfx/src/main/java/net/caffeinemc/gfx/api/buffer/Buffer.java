package net.caffeinemc.gfx.api.buffer;

/**
 * A buffer object represents a contiguous array of memory, stored either on the client or server. Buffer objects can
 * (for example) be used to store data for vertex, uniform, or shader storage arrays. They can also be used to read-back
 * data from the server on the client.
 */
public interface Buffer {
    /**
     * @return The capacity (in bytes) of the buffer.
     */
    long capacity();
}
