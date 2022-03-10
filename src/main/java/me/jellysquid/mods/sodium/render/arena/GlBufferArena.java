package me.jellysquid.mods.sodium.render.arena;

import net.caffeinemc.gfx.api.buffer.Buffer;

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
