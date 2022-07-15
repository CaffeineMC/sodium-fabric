package net.caffeinemc.sodium.render.buffer.arena;

import net.caffeinemc.gfx.api.buffer.Buffer;

import java.util.List;

public interface ArenaBuffer {
    long getDeviceUsedMemory();

    long getDeviceAllocatedMemory();

    void free(long key);

    void delete();
    
    void reset();

    boolean isEmpty();

    Buffer getBufferObject();

    void upload(List<PendingUpload> uploads, int frameIndex);

    int getStride();
}
