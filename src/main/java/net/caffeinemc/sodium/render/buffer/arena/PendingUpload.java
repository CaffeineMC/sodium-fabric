package net.caffeinemc.sodium.render.buffer.arena;

import java.util.concurrent.atomic.AtomicLong;
import net.caffeinemc.sodium.render.chunk.RenderSection;
import net.caffeinemc.sodium.util.NativeBuffer;

public class PendingUpload {
    public final RenderSection section;
    public final NativeBuffer data;
    public final AtomicLong bufferSegmentResult = new AtomicLong(BufferSegment.INVALID);

    public PendingUpload(RenderSection section, NativeBuffer data) {
        this.section = section;
        this.data = data;
    }
}
