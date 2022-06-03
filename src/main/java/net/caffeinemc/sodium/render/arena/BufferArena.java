package net.caffeinemc.sodium.render.arena;

import net.caffeinemc.gfx.api.buffer.Buffer;

import java.util.List;

public interface BufferArena {
    long getDeviceUsedMemory();

    long getDeviceAllocatedMemory();

    void free(BufferSegment entry);

    void delete();

    boolean isEmpty();

    Buffer getBufferObject();

    void upload(List<PendingUpload> uploads, int frameIndex);

    int getStride();
}
