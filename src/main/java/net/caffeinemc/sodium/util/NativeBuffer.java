package net.caffeinemc.sodium.util;

import it.unimi.dsi.fastutil.objects.Reference2ReferenceMap;
import it.unimi.dsi.fastutil.objects.Reference2ReferenceMaps;
import it.unimi.dsi.fastutil.objects.Reference2ReferenceOpenHashMap;
import java.lang.ref.PhantomReference;
import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.stream.Collectors;
import net.caffeinemc.sodium.SodiumClientMod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.system.MemoryUtil;

public class NativeBuffer {
    private static final Logger LOGGER = LogManager.getLogger(NativeBuffer.class);

    private static final ReferenceQueue<NativeBuffer> RECLAIM_QUEUE = new ReferenceQueue<>();
    private static final Reference2ReferenceMap<Reference<NativeBuffer>, BufferReference> ACTIVE_BUFFERS =
            Reference2ReferenceMaps.synchronize(new Reference2ReferenceOpenHashMap<>());

    private static long ALLOCATED = 0L;

    private final BufferReference ref;

    public NativeBuffer(int capacity) {
        this.ref = allocate(capacity);

        ACTIVE_BUFFERS.put(new PhantomReference<>(this, RECLAIM_QUEUE), this.ref);
    }

    public static NativeBuffer copy(ByteBuffer src) {
        NativeBuffer dst = new NativeBuffer(src.remaining());
        MemoryUtil.memCopy(src, dst.getDirectBuffer());
        return dst;
    }

    public ByteBuffer getDirectBuffer() {
        this.ref.checkFreed();

        return MemoryUtil.memByteBuffer(this.ref.address, this.ref.length);
    }

    public long getAddress() {
        this.ref.checkFreed();
        
        return this.ref.address;
    }
    
    public int getLength() {
        return this.ref.length;
    }

    public void free() {
        deallocate(this.ref);
    }

    public static void reclaim(boolean forceGc) {
        if (forceGc) {
            System.gc();
        }

        Reference<? extends NativeBuffer> ref;

        while ((ref = RECLAIM_QUEUE.poll()) != null) {
            BufferReference buf = ACTIVE_BUFFERS.remove(ref);

            if (buf.freed) {
                continue;
            }

            deallocate(buf);

            if (buf.allocationSite != null) {
                LOGGER.warn("Reclaimed {} bytes at address {} that were leaked from allocation site:\n{}",
                            buf.length, buf.address,
                            Arrays.stream(buf.allocationSite)
                                  .map(StackWalker.StackFrame::toString)
                                  .collect(Collectors.joining("\n")));
            } else {
                LOGGER.warn("Reclaimed {} bytes at address {} that were leaked from an unknown location (logging is disabled)",
                        buf.length, buf.address);
            }
        }
    }
    
    private static final int UNNEEDED_STACK_FRAMES = 3;
    
    private static StackWalker.StackFrame[] getStackTrace() {
        if (SodiumClientMod.options().advanced.enableMemoryTracing) {
            return StackWalker.getInstance()
                              .walk(stackFrameStream -> stackFrameStream.skip(UNNEEDED_STACK_FRAMES)
                                                                        .toArray(StackWalker.StackFrame[]::new));
        }
        return null;
    }

    public static long getTotalAllocated() {
        return ALLOCATED;
    }

    private static final int MAX_ALLOCATION_ATTEMPTS = 3;

    private static BufferReference allocate(int bytes) {
        long address = 0;
        int attempts = 0;

        while (++attempts <= MAX_ALLOCATION_ATTEMPTS) {
            address = MemoryUtil.nmemAlloc(bytes);

            if (address != MemoryUtil.NULL) {
                break;
            }

            LOGGER.error("EMERGENCY: Tried to allocate {} bytes but the allocator reports failure", bytes);
            LOGGER.error("EMERGENCY: ... Attempting to force a garbage collection cycle (attempt {}/{})", attempts, MAX_ALLOCATION_ATTEMPTS);

            // If memory allocation fails, force a garbage collection
            reclaim(true);
        }

        if (address == MemoryUtil.NULL) {
            throw new OutOfMemoryError("Couldn't allocate %s bytes after %s attempts".formatted(bytes, attempts));
        }
    
        StackWalker.StackFrame[] stackTrace = getStackTrace();

        BufferReference ref = new BufferReference(address, bytes, stackTrace);
        ALLOCATED += ref.length;

        return ref;
    }

    private static void deallocate(BufferReference ref) {
        ref.checkFreed();
        ref.freed = true;

        MemoryUtil.nmemFree(ref.address);

        ALLOCATED -= ref.length;
    }

    private static class BufferReference {
        public final long address;
        public final int length;

        public final StackWalker.StackFrame[] allocationSite;

        public boolean freed;

        private BufferReference(long address, int length, StackWalker.StackFrame[] allocationSite) {
            this.address = address;
            this.length = length;
            this.allocationSite = allocationSite;
        }

        private void checkFreed() {
            if (this.freed) {
                throw new IllegalStateException("Buffer has been deleted");
            }
        }
    }
}
