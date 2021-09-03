package me.jellysquid.mods.sodium.render.chunk.arena;

import me.jellysquid.mods.thingl.buffer.GlBuffer;
import me.jellysquid.mods.thingl.device.CommandList;

import java.util.stream.Stream;

public interface GlBufferArena {
    int getDeviceUsedMemory();

    int getDeviceAllocatedMemory();

    void free(GlBufferSegment entry);

    void delete(CommandList commands);

    boolean isEmpty();

    GlBuffer getBufferObject();

    boolean upload(CommandList commandList, Stream<PendingUpload> stream);
}
