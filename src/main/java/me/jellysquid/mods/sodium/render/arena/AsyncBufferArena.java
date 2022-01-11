package me.jellysquid.mods.sodium.render.arena;

import me.jellysquid.mods.sodium.opengl.buffer.Buffer;
import me.jellysquid.mods.sodium.opengl.device.RenderDevice;
import me.jellysquid.mods.sodium.render.stream.StreamingBuffer;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

// TODO: handle alignment
// TODO: handle element vs pointers
public class AsyncBufferArena implements GlBufferArena {
    static final boolean CHECK_ASSERTIONS = false;

    private final int resizeIncrement;

    private final RenderDevice device;
    private final StreamingBuffer stagingBuffer;
    private Buffer arenaBuffer;

    private GlBufferSegment head;

    private int capacity;
    private int used;

    private final int stride;

    public AsyncBufferArena(RenderDevice device, int capacity, int stride, StreamingBuffer stagingBuffer) {
        this.device = device;
        this.resizeIncrement = capacity / 16;
        this.capacity = capacity;

        this.head = new GlBufferSegment(this, 0, capacity);
        this.head.setFree(true);

        this.arenaBuffer = device.createBuffer((long) capacity * stride);
        this.stagingBuffer = stagingBuffer;

        this.stride = stride;
    }

    private void resize(int newCapacity) {
        if (this.used > newCapacity) {
            throw new UnsupportedOperationException("New capacity must be larger than used size");
        }

        this.checkAssertions();

        int base = newCapacity - this.used;

        List<GlBufferSegment> usedSegments = this.getUsedSegments();
        List<PendingBufferCopyCommand> pendingCopies = this.buildTransferList(usedSegments, base);

        this.transferSegments(pendingCopies, newCapacity);

        this.head = new GlBufferSegment(this, 0, base);
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

            if (currentCopyCommand == null || currentCopyCommand.readOffset + currentCopyCommand.length != s.getOffset()) {
                if (currentCopyCommand != null) {
                    pendingCopies.add(currentCopyCommand);
                }

                currentCopyCommand = new PendingBufferCopyCommand(s.getOffset(), writeOffset, s.getLength());
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
        var srcBufferObj = this.arenaBuffer;
        var dstBufferObj = this.device.createBuffer(this.toBytes(capacity));

        for (PendingBufferCopyCommand cmd : list) {
            this.device.copyBuffer(this.toBytes(cmd.length), srcBufferObj, this.toBytes(cmd.readOffset), dstBufferObj, this.toBytes(cmd.writeOffset));
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
        return this.toBytes(this.used);
    }

    @Override
    public int getDeviceAllocatedMemory() {
        return this.toBytes(this.capacity);
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
    public void upload(Stream<PendingUpload> stream) {
        // A linked list is used as we'll be randomly removing elements and want O(1) performance
        List<PendingUpload> queue = stream.collect(Collectors.toCollection(LinkedList::new));

        // Try to upload all of the data into free segments first
        queue.removeIf(this::tryUpload);

        // If we weren't able to upload some buffers, they will have been left behind in the queue
        if (!queue.isEmpty()) {
            // Calculate the amount of memory needed for the remaining uploads
            int remainingElements = queue.stream()
                    .mapToInt(upload -> this.toElements(upload.getDataBuffer().getLength()))
                    .sum();

            // Ask the arena to grow to accommodate the remaining uploads
            // This will force a re-allocation and compaction, which will leave us a continuous free segment
            // for the remaining uploads
            this.ensureCapacity(remainingElements);

            // Try again to upload any buffers that failed last time
            queue.removeIf(this::tryUpload);

            // If we still had failures, something has gone wrong
            if (!queue.isEmpty()) {
                throw new RuntimeException("Failed to upload all buffers");
            }
        }
    }

    private boolean tryUpload(PendingUpload upload) {
        ByteBuffer data = upload.getDataBuffer()
                .getDirectBuffer();

        int bytes = data.remaining();

        GlBufferSegment dst = this.alloc(this.toElements(bytes));

        if (dst == null) {
            return false;
        }

        // Copy the data into our staging buffer, then copy it into the arena's buffer
        var readHandle = this.stagingBuffer.write(data);

        try {
            this.device.copyBuffer(bytes, readHandle.getBuffer(), readHandle.getOffset(), this.arenaBuffer, this.toBytes(dst.getOffset()));
            upload.setResult(dst);
        } finally {
            readHandle.free();
        }

        return true;
    }

    public void ensureCapacity(int elementCount) {
        // Re-sizing the arena results in a compaction, so any free space in the arena will be
        // made into one contiguous segment, joined with the new segment of free space we're asking for
        // We calculate the number of free elements in our arena and then subtract that from the total requested
        int elementsNeeded = elementCount - (this.capacity - this.used);

        // Try to allocate some extra buffer space unless this is an unusually large allocation
        this.resize(Math.max(this.capacity + this.resizeIncrement, this.capacity + elementsNeeded));
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


    private int toBytes(int index) {
        return index * this.stride;
    }

    private int toElements(int bytes) {
        return bytes / this.stride;
    }
}
