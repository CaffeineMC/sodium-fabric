package me.jellysquid.mods.sodium.render.chunk.arena;

import me.jellysquid.mods.thingl.buffer.GlBuffer;

import java.util.stream.Stream;

public interface GlBufferArena {
    int getDeviceUsedMemory();

    int getDeviceAllocatedMemory();

    void free(GlBufferSegment entry);

    void delete();

    boolean isEmpty();

    GlBuffer getBufferObject();

    boolean upload(Stream<PendingUpload> stream);
}
