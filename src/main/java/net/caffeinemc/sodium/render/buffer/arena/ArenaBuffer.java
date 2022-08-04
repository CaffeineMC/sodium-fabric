package net.caffeinemc.sodium.render.buffer.arena;

import it.unimi.dsi.fastutil.longs.LongSortedSet;
import javax.annotation.Nullable;
import net.caffeinemc.gfx.api.buffer.Buffer;

import java.util.List;

public interface ArenaBuffer {
    long getDeviceUsedMemory();

    long getDeviceAllocatedMemory();

    void free(long key);

    void delete();
    
    /**
     * @return either the segments of the buffer that were removed, sorted by offset,
     *         or null if the compaction did nothing.
     */
    @Nullable
    LongSortedSet compact();
    
    float getFragmentation();
    
    void reset();

    boolean isEmpty();

    Buffer getBufferObject();

    void upload(List<PendingUpload> uploads, int frameIndex);

    int getStride();
}
