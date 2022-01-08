package me.jellysquid.mods.sodium.render.stream;

import me.jellysquid.mods.sodium.opengl.buffer.Buffer;

import java.nio.ByteBuffer;

public interface StreamingBuffer {

    /**
     * Shorthand for {@link #allocate(long, long)} where the alignment is 1.
     */
    default ByteBuffer allocate(long capacity) {
        return this.allocate(capacity, 1);
    }

    /**
     * Creates a logical region of the buffer that can be written to. When finished
     * writing to the region, make sure to call {@link #flushRegion(ByteBuffer)}. When
     * finished calling all draw calls that rely on the region, make sure to call
     * {@link #fenceRegion(ByteBuffer)}.
     *
     * @param capacity The capacity of the region.
     * @param alignment The multiple in bytes that the start of the region needs to be aligned to.
     * @return A pointer in the form of a ByteBuffer to the region, where the position is kept and
     *         the limit is set to the position + capacity. TODO: that's kinda gross, now the capacity is larger than it should be
     * @throws OutOfMemoryError When the specified capacity is larger than the buffer itself.
     */
    ByteBuffer allocate(long capacity, long alignment);

    Buffer getBuffer();

    /**
     * Flushes the data written to the region to the GPU. This should be called before
     * calling any draw calls that rely on the contents of the region.
     *
     * @param region The region provided by a call to {@link #allocate(long, long)}
     */
    void flushRegion(ByteBuffer region);

    /**
     *
     * @param region The region provided by a call to {@link #allocate(long, long)}
     */
    void fenceRegion(ByteBuffer region);

    void delete();

    String getDebugString();
}
