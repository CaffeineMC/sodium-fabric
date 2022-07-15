package net.caffeinemc.sodium.render.buffer.arena;

import java.util.concurrent.atomic.AtomicLong;
import net.caffeinemc.sodium.util.NativeBuffer;

import java.util.concurrent.atomic.AtomicReference;

public class PendingUpload {
    public final NativeBuffer data;
    public final AtomicLong bufferSegmentHolder = new AtomicLong(BufferSegment.INVALID);

    public PendingUpload(NativeBuffer data) {
        this.data = data;
    }
}
