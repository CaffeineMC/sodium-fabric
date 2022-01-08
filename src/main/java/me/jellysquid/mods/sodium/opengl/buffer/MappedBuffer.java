package me.jellysquid.mods.sodium.opengl.buffer;

import java.nio.ByteBuffer;

public interface MappedBuffer extends Buffer {
    ByteBuffer getPointer();
}
