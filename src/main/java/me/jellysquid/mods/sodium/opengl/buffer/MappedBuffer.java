package me.jellysquid.mods.sodium.opengl.buffer;

import java.nio.ByteBuffer;

public interface MappedBuffer extends Buffer {
    void write(int writeOffset, ByteBuffer data);

    void flush(int pos, int length);

    ByteBuffer getView();
}
