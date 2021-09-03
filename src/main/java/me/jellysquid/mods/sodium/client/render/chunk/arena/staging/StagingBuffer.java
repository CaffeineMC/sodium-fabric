package me.jellysquid.mods.sodium.client.render.chunk.arena.staging;

import me.jellysquid.mods.thingl.buffer.GlBuffer;
import me.jellysquid.mods.thingl.device.CommandList;

import java.nio.ByteBuffer;

public interface StagingBuffer {
    void enqueueCopy(CommandList commandList, ByteBuffer data, GlBuffer dst, long writeOffset);

    void flush(CommandList commandList);

    void delete(CommandList commandList);

    void flip();
}
