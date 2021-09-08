package me.jellysquid.mods.thingl.buffer;

import java.nio.ByteBuffer;

public interface BufferMapping {
    void write(ByteBuffer data, int readOffset);
}
