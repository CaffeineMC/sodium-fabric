package net.caffeinemc.sodium.render.arena;

import net.caffeinemc.sodium.util.NativeBuffer;

import java.util.concurrent.atomic.AtomicReference;

public class PendingUpload {
    public final NativeBuffer data;
    public final AtomicReference<BufferSegment> holder = new AtomicReference<>();

    public PendingUpload(NativeBuffer data) {
        this.data = data;
    }
}
