package net.caffeinemc.gfx.api.buffer;

import java.nio.ByteBuffer;

public interface MappedBuffer extends Buffer {
    void write(int writeOffset, ByteBuffer data);

    void flush(long pos, long length);

    ByteBuffer getView();

    default void flush() {
        this.flush(0, this.getCapacity());
    }
}
