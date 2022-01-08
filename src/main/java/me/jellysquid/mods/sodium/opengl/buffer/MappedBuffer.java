package me.jellysquid.mods.sodium.opengl.buffer;

import java.nio.ByteBuffer;

public interface MappedBuffer extends Buffer {
    void write(ByteBuffer data, int writeOffset);

    void flush(int pos, int length);
}
