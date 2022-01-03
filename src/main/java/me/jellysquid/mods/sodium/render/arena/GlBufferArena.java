package me.jellysquid.mods.sodium.render.arena;

import me.jellysquid.mods.sodium.opengl.buffer.Buffer;

import java.util.stream.Stream;

public interface GlBufferArena {
    int getDeviceUsedMemory();

    int getDeviceAllocatedMemory();

    void free(GlBufferSegment entry);

    void delete();

    boolean isEmpty();

    Buffer getBufferObject();

    void upload(Stream<PendingUpload> stream);
}
