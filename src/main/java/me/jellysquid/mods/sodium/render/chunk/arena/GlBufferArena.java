package me.jellysquid.mods.sodium.render.chunk.arena;

import me.jellysquid.mods.sodium.opengl.buffer.GlBuffer;
import me.jellysquid.mods.sodium.opengl.device.CommandList;

import java.util.stream.Stream;

public interface GlBufferArena {
    int getDeviceUsedMemory();

    int getDeviceAllocatedMemory();

    void free(GlBufferSegment entry);

    void delete(CommandList commands);

    boolean isEmpty();

    GlBuffer getBufferObject();

    void upload(CommandList commandList, Stream<PendingUpload> stream);
}
