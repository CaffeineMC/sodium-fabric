package net.caffeinemc.sodium.render.arena;

import java.util.concurrent.atomic.AtomicReference;

public record PendingTransfer(AtomicReference<BufferSegment> holder, long offset, long length) {
}
