package me.jellysquid.mods.sodium.render.chunk.arena.staging;

import me.jellysquid.mods.thingl.buffer.Buffer;

import java.nio.ByteBuffer;

public interface StagingBuffer {
    void enqueueCopy(ByteBuffer data, Buffer dst, long writeOffset);

    void flush();

    void delete();

    void flip();
}
