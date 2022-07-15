package net.caffeinemc.sodium.render.buffer.arena;

import java.util.concurrent.atomic.AtomicLong;

public record PendingTransfer(AtomicLong bufferSegmentHolder, long offset, long length) {
}
