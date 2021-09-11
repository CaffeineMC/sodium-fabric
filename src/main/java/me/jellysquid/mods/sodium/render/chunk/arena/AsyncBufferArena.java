package me.jellysquid.mods.sodium.render.chunk.arena;

import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import me.jellysquid.mods.sodium.render.chunk.arena.staging.StagingBuffer;
import me.jellysquid.mods.thingl.buffer.*;
import me.jellysquid.mods.thingl.device.RenderDevice;
import org.lwjgl.system.MemoryUtil;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

// TODO: handle alignment
// TODO: handle element vs pointers
public class AsyncBufferArena implements GlBufferArena {
    static final boolean CHECK_ASSERTIONS = false;

    private static final BufferUsage BUFFER_USAGE = BufferUsage.STATIC_DRAW;

    private final int resizeIncrement;

    private final RenderDevice device;
    private MutableBuffer arenaBuffer;

    private GlBufferSegment head;

    private int capacity;
    private int used;

    public AsyncBufferArena(RenderDevice device, int initialCapacity) {
        this.resizeIncrement = initialCapacity / 16;
        this.capacity = initialCapacity;

        this.head = new GlBufferSegment(this, 0, initialCapacity);
        this.head.setFree(true);

        this.device = device;
        this.arenaBuffer = device.createMutableBuffer();
        device.allocateStorage(this.arenaBuffer, initialCapacity, BUFFER_USAGE);
    }

    // Re-sizing the arena results in a compaction, so any free space in the arena will be
    // made into one contiguous segment and placed at the start of the buffer afterwards.
    private void resize(int newCapacity) {
        if (this.used > newCapacity) {
            throw new UnsupportedOperationException("New capacity must be larger than used size");
        }

        this.checkAssertions();

        int freeBytes = newCapacity - this.used;

        List<GlBufferSegment> usedSegments = this.getUsedSegments();
        List<PendingBufferCopyCommand> pendingCopies = this.buildTransferList(usedSegments, freeBytes);

        this.transferSegments(pendingCopies, newCapacity);

        this.head = new GlBufferSegment(this, 0, freeBytes);
        this.head.setFree(true);

        if (usedSegments.isEmpty()) {
            this.head.setNext(null);
        } else {
            this.head.setNext(usedSegments.get(0));
            this.head.getNext()
                    .setPrev(this.head);
        }

        this.checkAssertions();
    }

    private List<PendingBufferCopyCommand> buildTransferList(List<GlBufferSegment> usedSegments, int base) {
        List<PendingBufferCopyCommand> pendingCopies = new ArrayList<>();
        PendingBufferCopyCommand currentCopyCommand = null;

        int writeOffset = base;

        for (int i = 0; i < usedSegments.size(); i++) {
            GlBufferSegment s = usedSegments.get(i);

            if (currentCopyCommand == null || currentCopyCommand.writeOffset + currentCopyCommand.length != s.getOffset() || !s.isUploaded()) {
                if (currentCopyCommand != null) {
                    pendingCopies.add(currentCopyCommand);
                }

                if (s.isUploaded()) {
                    currentCopyCommand = new PendingBufferCopyCommand(s.getOffset(), writeOffset, s.getLength());
                }
            } else {
                currentCopyCommand.length += s.getLength();
            }

            s.setOffset(writeOffset);

            if (i + 1 < usedSegments.size()) {
                s.setNext(usedSegments.get(i + 1));
            } else {
                s.setNext(null);
            }

            if (i - 1 < 0) {
                s.setPrev(null);
            } else {
                s.setPrev(usedSegments.get(i - 1));
            }

            writeOffset += s.getLength();
        }

        if (currentCopyCommand != null) {
            pendingCopies.add(currentCopyCommand);
        }

        return pendingCopies;
    }

    private void transferSegments(Collection<PendingBufferCopyCommand> list, int capacity) {
        MutableBuffer srcBufferObj = this.arenaBuffer;
        MutableBuffer dstBufferObj = this.device.createMutableBuffer();

        this.device.allocateStorage(dstBufferObj, capacity, BUFFER_USAGE);

        for (PendingBufferCopyCommand cmd : list) {
            this.device.copyBufferSubData(srcBufferObj, dstBufferObj,
                    cmd.readOffset,
                    cmd.writeOffset,
                    cmd.length);
        }

        this.device.deleteBuffer(srcBufferObj);

        this.arenaBuffer = dstBufferObj;
        this.capacity = capacity;
    }

    private ArrayList<GlBufferSegment> getUsedSegments() {
        ArrayList<GlBufferSegment> used = new ArrayList<>();
        GlBufferSegment seg = this.head;

        while (seg != null) {
            GlBufferSegment next = seg.getNext();

            if (!seg.isFree()) {
                used.add(seg);
            }

            seg = next;
        }

        return used;
    }

    @Override
    public int getDeviceUsedMemory() {
        return this.used;
    }

    @Override
    public int getDeviceAllocatedMemory() {
        return this.capacity;
    }

    private GlBufferSegment alloc(int size) {
        GlBufferSegment a = this.findFree(size);

        if (a == null) {
            return null;
        }

        GlBufferSegment result;

        if (a.getLength() == size) {
            a.setFree(false);

            result = a;
        } else {
            GlBufferSegment b = new GlBufferSegment(this, a.getEnd() - size, size);
            b.setNext(a.getNext());
            b.setPrev(a);

            if (b.getNext() != null) {
                b.getNext()
                        .setPrev(b);
            }

            a.setLength(a.getLength() - size);
            a.setNext(b);

            result = b;
        }

        this.used += result.getLength();
        this.checkAssertions();

        return result;
    }

    private GlBufferSegment findFree(int size) {
        GlBufferSegment entry = this.head;
        GlBufferSegment best = null;

        while (entry != null) {
            if (entry.isFree()) {
                if (entry.getLength() == size) {
                    return entry;
                } else if (entry.getLength() >= size) {
                    if (best == null || best.getLength() > entry.getLength()) {
                        best = entry;
                    }
                }
            }

            entry = entry.getNext();
        }

        return best;
    }

    @Override
    public void free(GlBufferSegment entry) {
        if (entry.isFree()) {
            throw new IllegalStateException("Already freed");
        }

        entry.setFree(true);

        this.used -= entry.getLength();

        GlBufferSegment next = entry.getNext();

        if (next != null && next.isFree()) {
            entry.mergeInto(next);
        }

        GlBufferSegment prev = entry.getPrev();

        if (prev != null && prev.isFree()) {
            prev.mergeInto(entry);
        }

        this.checkAssertions();
    }

    @Override
    public void delete() {
        this.device.deleteBuffer(this.arenaBuffer);
    }

    @Override
    public boolean isEmpty() {
        return this.used <= 0;
    }

    @Override
    public Buffer getBufferObject() {
        return this.arenaBuffer;
    }

    @Override
    public boolean upload(StagingBuffer stagingBuffer, Stream<PendingUpload> stream) {
        // Record the buffer object before we start any work
        // If the arena needs to re-allocate a buffer, this will allow us to check and return an appropriate flag
        Buffer buffer = this.arenaBuffer;

        Map<PendingUpload, GlBufferSegment> allocations = new Reference2ObjectOpenHashMap<>();

        // We need to first accumulate the incoming uploads into a list, as we iterate over it twice
        // in order to determine the total payload size. The queue is sorted from largest to smallest so that
        // free space is re-used packed as efficiently as possible.
        List<PendingUpload> queue = stream
                .collect(Collectors.toList());

        // If we don't have enough free bytes in the arena, then we can avoid unnecessarily packing data
        // into the remaining space by compacting and re-allocating now. We don't force the re-allocation here
        // as we might have enough non-continuous memory available to service all allocations.
        this.ensureCapacity(queue, false);

        // First stage: Reserve space for as many payloads as possible, with whatever couldn't
        // be packed into the existing free regions being left in the queue
        queue.removeIf(upload -> this.reserve(allocations, upload));

        // Second stage: There wasn't enough continuous regions of memory to reserve, so re-allocate &
        // compact the arena, then try reserving again for any payloads that failed previously.
        if (!queue.isEmpty()) {
            this.ensureCapacity(queue, true);

            // Attempt to reserve space for everything else, since we should have enough available memory now
            queue.removeIf(upload -> this.reserve(allocations, upload));
        }

        // At the end of everything, we should've been able to reserve space for all uploads
        if (!queue.isEmpty()) {
            throw new RuntimeException("Failed to allocate space for all buffers");
        }

        // Finally, upload all the data into the reserved regions
        for (Map.Entry<PendingUpload, GlBufferSegment> entry : allocations.entrySet()) {
            PendingUpload upload = entry.getKey();
            GlBufferSegment segment = entry.getValue();

            var data = upload.getDataBuffer();

            // Copy the data into our staging buffer, which then copies it into the arena's buffer
            stagingBuffer.enqueueCopy(data.getDirectBuffer(), this.arenaBuffer, segment.getOffset());
            upload.setResult(segment);

            segment.setUploaded();
        }

        return this.arenaBuffer != buffer;
    }

    private boolean reserve(Map<PendingUpload, GlBufferSegment> reserved, PendingUpload upload) {
        GlBufferSegment seg = this.alloc(upload.getLength());

        if (seg != null) {
            reserved.put(upload, seg);
        }

        return seg != null;
    }

    private void ensureCapacity(List<PendingUpload> queue, boolean force) {
        int bytes = 0;

        for (PendingUpload upload : queue) {
            bytes += upload.getLength();
        }

        int free = this.getFree();

        if (force || bytes > free) {
            int next = this.capacity + this.resizeIncrement;
            int minimum = this.capacity + (bytes - free);

            // Try to allocate some extra buffer space unless this is an unusually large allocation
            this.resize(Math.max(next, minimum));
        }
    }

    private int getFree() {
        return this.capacity - this.used;
    }

    private void checkAssertions() {
        if (CHECK_ASSERTIONS) {
            this.checkAssertions0();
        }
    }

    private void checkAssertions0() {
        GlBufferSegment seg = this.head;
        int used = 0;

        while (seg != null) {
            if (seg.getOffset() < 0) {
                throw new IllegalStateException("segment.start < 0: out of bounds");
            } else if (seg.getEnd() > this.capacity) {
                throw new IllegalStateException("segment.end > arena.capacity: out of bounds");
            }

            if (!seg.isFree()) {
                used += seg.getLength();
            }

            GlBufferSegment next = seg.getNext();

            if (next != null) {
                if (next.getOffset() < seg.getEnd()) {
                    throw new IllegalStateException("segment.next.start < segment.end: overlapping segments (corrupted)");
                } else if (next.getOffset() > seg.getEnd()) {
                    throw new IllegalStateException("segment.next.start > segment.end: not truly connected (sparsity error)");
                }

                if (next.isFree() && next.getNext() != null) {
                    if (next.getNext().isFree()) {
                        throw new IllegalStateException("segment.free && segment.next.free: not merged consecutive segments");
                    }
                }
            }

            GlBufferSegment prev = seg.getPrev();

            if (prev != null) {
                if (prev.getEnd() > seg.getOffset()) {
                    throw new IllegalStateException("segment.prev.end > segment.start: overlapping segments (corrupted)");
                } else if (prev.getEnd() < seg.getOffset()) {
                    throw new IllegalStateException("segment.prev.end < segment.start: not truly connected (sparsity error)");
                }

                if (prev.isFree() && prev.getPrev() != null) {
                    if (prev.getPrev().isFree()) {
                        throw new IllegalStateException("segment.free && segment.prev.free: not merged consecutive segments");
                    }
                }
            }

            seg = next;
        }

        if (this.used < 0) {
            throw new IllegalStateException("arena.used < 0: failure to track");
        } else if (this.used > this.capacity) {
            throw new IllegalStateException("arena.used > arena.capacity: failure to track");
        }

        if (this.used != used) {
            throw new IllegalStateException("arena.used is invalid");
        }
    }

}
