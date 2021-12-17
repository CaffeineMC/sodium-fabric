package me.jellysquid.mods.sodium.client.gl.arena;

import me.jellysquid.mods.sodium.client.gl.buffer.GlBuffer;
import me.jellysquid.mods.sodium.client.gl.device.CommandList;

import java.nio.ByteBuffer;

public interface GlBufferArena {
    int getDeviceUsedMemory();

    int getDeviceAllocatedMemory();

    void free(GlBufferSegment entry);

    void delete(CommandList commands);

    boolean isEmpty();

    GlBuffer getBufferObject();

    GlBufferSegment upload(CommandList commandList, ByteBuffer data);
}
