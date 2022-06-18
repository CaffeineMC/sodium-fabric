package net.caffeinemc.sodium.render.buffer.arena;

import java.util.concurrent.atomic.AtomicReference;

public record PendingTransfer(AtomicReference<BufferSegment> holder, long offset, long length) {
}
