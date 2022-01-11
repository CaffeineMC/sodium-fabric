package me.jellysquid.mods.sodium.render.stream;

import me.jellysquid.mods.sodium.opengl.buffer.Buffer;

import java.nio.ByteBuffer;

public interface StreamingBuffer {
    default Handle write(ByteBuffer data) {
        return this.write(data, 1);
    }

    /**
     * Writes the provided data into this streaming buffer, and returns a handle to the newly written data in this
     * buffer. The caller must then invoke {@link Handle#free()} when they are done using the memory.
     *
     * @param data The data to write into this buffer
     * @param alignment The alignment at which the written data will start at
     * @return A handle to the written data at the specified alignment
     */
    Handle write(ByteBuffer data, int alignment);

    /**
     * Provides a writer instance which can be used to write data directly into this buffer. The caller must invoke
     * {@link Writer#finish()} after writing all their data, which will return a handle to the newly written data.
     *
     * @param length The maximum capacity of the writer (i.e. space to reserve)
     * @param alignment The alignment at which the written data will start at
     * @return A writer instance of the specified capacity and alignment
     */
    Writer write(int length, int alignment);

    /**
     * Provides a hint to the implementation that it should create a synchronization point in the current command
     * stream for the previously written data.
     *
     * Note: The implementation is allowed to automatically perform flushing of inactive data segments, but this can
     * result in a synchronization point being created too late in the command stream, which can then result in
     * pipeline stalls.
     *
     * However, calling this method at the end of your rendering commands allows the implementation to create a
     * synchronization point sooner in the command stream, which can avoid this costly pipeline stall.
     *
     * Advice: You should call this once at the end of every frame so that stalls longer than a frame will
     * never happen.
     */
    void flush();

    /**
     * Deletes this buffer and any resources attached to it.
     */
    void delete();

    String getDebugString();

    interface Handle {
        Buffer getBuffer();

        int getOffset();
        int getLength();

        void free();
    }

    interface Writer {
        long next(int length);

        Handle finish();
    }
}
