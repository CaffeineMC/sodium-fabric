package me.jellysquid.mods.sodium.render.chunk.arena.staging;

import me.jellysquid.mods.thingl.buffer.GlBuffer;

import java.nio.ByteBuffer;

public interface StagingBuffer {
    void enqueueCopy(ByteBuffer data, GlBuffer dst, long writeOffset);

    void flush();

    void delete();

    void flip();
}
