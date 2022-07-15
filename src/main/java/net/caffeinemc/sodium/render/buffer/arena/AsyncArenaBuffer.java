package net.caffeinemc.sodium.render.buffer.arena;

import it.unimi.dsi.fastutil.ints.Int2ReferenceAVLTreeMap;
import java.util.EnumSet;
import java.util.LinkedList;
import java.util.List;
import net.caffeinemc.gfx.api.buffer.Buffer;
import net.caffeinemc.gfx.api.buffer.ImmutableBufferFlags;
import net.caffeinemc.gfx.api.device.RenderDevice;
import net.caffeinemc.gfx.util.buffer.StreamingBuffer;
import org.lwjgl.system.MemoryUtil;

// TODO: handle alignment
// TODO: handle element vs pointers
// TODO: convert to longs
public class AsyncArenaBuffer implements ArenaBuffer {
    static final boolean CHECK_ASSERTIONS = false;

    private final int resizeIncrement;

    private final RenderDevice device;
    private final StreamingBuffer stagingBuffer;
    private Buffer arenaBuffer;

    private Int2ReferenceAVLTreeMap<FreedSegment> freedSegments = new Int2ReferenceAVLTreeMap<>();
    private FreedSegment head;

    private int capacity;
    private int position;
    private int used;

    private final int stride;

    public AsyncArenaBuffer(RenderDevice device, StreamingBuffer stagingBuffer, int capacity, int stride) {
        this.device = device;
        this.stagingBuffer = stagingBuffer;
        this.resizeIncrement = capacity / 8;
        this.capacity = capacity;
        this.arenaBuffer = device.createBuffer((long) capacity * stride, EnumSet.noneOf(ImmutableBufferFlags.class));
        this.stride = stride;
    }
    
    public void reset() {
        this.head = null;
        this.used = 0;
        this.position = 0;
    }

    private void resize(int newCapacity) {
        if (this.used > newCapacity) {
            throw new UnsupportedOperationException("New capacity must be larger than used size");
        }

        this.checkAssertions();
    
        var srcBufferObj = this.arenaBuffer;
        var dstBufferObj = this.device.createBuffer(this.toBytes(this.capacity), EnumSet.noneOf(ImmutableBufferFlags.class));
        
        this.device.copyBuffer(
                srcBufferObj,
                dstBufferObj,
                0,
                0,
                this.toBytes(this.position)
        );
    
        this.device.deleteBuffer(srcBufferObj);
    
        this.arenaBuffer = dstBufferObj;
        this.capacity = newCapacity;

        this.checkAssertions();
    }

    @Override
    public long getDeviceUsedMemory() {
        return this.toBytes(this.used);
    }

    @Override
    public long getDeviceAllocatedMemory() {
        return this.toBytes(this.capacity);
    }

    private long alloc(int size) {
        FreedSegment a = this.findFree(size);
        
        long result = BufferSegment.INVALID;

        if (a != null) {
            if (a.length == size) {
                result = BufferSegment.createKey(size, a.offset);
                
                // get rid of element from linked list
                if (a.next != null) {
                    a.next.prev = null;
                }
                if (a.prev != null) {
                    a.prev.next = a.next;
                }
            } else {
                result = BufferSegment.createKey(size, a.offset);
        
                a.offset += size;
            }
        } else if (this.capacity - this.position >= size) {
            result = BufferSegment.createKey(size, this.position);
            
            this.position += size;
        }

        // will be 0 if invalid
        this.used += BufferSegment.getLength(result);
        
        this.checkAssertions();

        return result;
    }

    private FreedSegment findFree(int size) {
        FreedSegment entry = this.head;
        FreedSegment best = null;

        while (entry != null) {
            if (entry.length == size) {
                return entry;
            } else if (entry.length >= size) {
                if (best == null || entry.length < best.length) {
                    best = entry;
                }
            }

            entry = entry.next;
        }

        return best;
    }

    @Override
    public void free(long key) {
        FreedSegment freedSegment = this.freedSegments.get(BufferSegment.getOffset(key));
        if (entry.isFree()) {
            throw new IllegalStateException("Already freed");
        }

        entry.setFree(true);

        this.used -= entry.length;

        BufferSegment next = entry.next;

        if (next != null && next.isFree()) {
            entry.mergeInto(next);
        }

        BufferSegment prev = entry.prev;

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
    public void upload(List<PendingUpload> uploads, int frameIndex) {
        // A linked list is used as we'll be randomly removing elements and want O(1) performance
        var pendingTransfers = new LinkedList<PendingTransfer>();

        var section = this.stagingBuffer.getSection(
                frameIndex,
                uploads.stream().mapToInt(u -> u.data.getLength()).sum(),
                true
        );

        // Write the PendingUploads to the mapped streaming buffer
        // Also create the pending transfers to go from streaming buffer -> arena buffer
        long sectionOffset = section.getDeviceOffset() + section.getView().position();
        // this is basically the address of what sectionOffset points to
        long sectionAddress = MemoryUtil.memAddress(section.getView());
        int transferOffset = 0;
        for (var upload : uploads) {
            int length = upload.data.getLength();
            pendingTransfers.add(
                    new PendingTransfer(
                            upload.bufferSegmentHolder,
                            sectionOffset + transferOffset,
                            length
                    )
            );

            MemoryUtil.memCopy(
                    upload.data.getAddress(),
                    sectionAddress + transferOffset,
                    length
            );

            transferOffset += length;
        }
        section.getView().position(section.getView().position() + transferOffset);
        section.flushPartial();

        var backingStreamingBuffer = this.stagingBuffer.getBufferObject();

        // Try to upload all the data into free segments first
        pendingTransfers.removeIf(transfer -> this.tryUpload(backingStreamingBuffer, transfer));

        // If we weren't able to upload some buffers, they will have been left behind in the queue
        if (!pendingTransfers.isEmpty()) {
            // Calculate the amount of memory needed for the remaining uploads
            int remainingElements = (int) pendingTransfers
                    .stream()
                    .mapToLong(transfer -> this.toElements(transfer.length()))
                    .sum();

            // Ask the arena to grow to accommodate the remaining uploads
            // This will force a re-allocation and compaction, which will leave us a continuous free segment
            // for the remaining uploads
            this.ensureCapacity(remainingElements);

            // Try again to upload any buffers that failed last time
            pendingTransfers.removeIf(transfer -> this.tryUpload(backingStreamingBuffer, transfer));

            // If we still had failures, something has gone wrong
            if (!pendingTransfers.isEmpty()) {
                throw new RuntimeException("Failed to upload all buffers");
            }
        }
    }

    private boolean tryUpload(Buffer streamingBuffer, PendingTransfer transfer) {
        long segment = this.alloc((int) this.toElements(transfer.length()));

        if (segment == BufferSegment.INVALID) {
            return false;
        }

        // Copy the uploads from the streaming buffer to the arena buffer
        this.device.copyBuffer(
                streamingBuffer,
                this.arenaBuffer,
                transfer.offset(),
                this.toBytes(BufferSegment.getOffset(segment)),
                transfer.length()
        );

        transfer.bufferSegmentHolder().set(segment);

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
        FreedSegment seg = this.head;
        int used = 0;

        while (seg != null) {
            if (seg.offset < 0) {
                throw new IllegalStateException("segment.start < 0: out of bounds");
            } else if (seg.getEnd() > this.capacity) {
                throw new IllegalStateException("segment.end > arena.capacity: out of bounds");
            }
    
            used += seg.length;
    
            FreedSegment next = seg.next;

            if (next != null) {
                if (next.offset < seg.getEnd()) {
                    throw new IllegalStateException("segment.next.start < segment.end: overlapping segments (corrupted)");
                } else if (next.offset > seg.getEnd()) {
                    throw new IllegalStateException("segment.next.start > segment.end: not truly connected (sparsity error)");
                }
            }
    
            FreedSegment prev = seg.prev;

            if (prev != null) {
                if (prev.getEnd() > seg.offset) {
                    throw new IllegalStateException("segment.prev.end > segment.start: overlapping segments (corrupted)");
                } else if (prev.getEnd() < seg.offset) {
                    throw new IllegalStateException("segment.prev.end < segment.start: not truly connected (sparsity error)");
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

    private long toBytes(long index) {
        return index * this.stride;
    }

    private long toElements(long bytes) {
        return bytes / this.stride;
    }

    @Override
    public int getStride() {
        return this.stride;
    }
    
    private static class FreedSegment {
    
        protected int offset;
        protected int length;
    
        protected FreedSegment next;
        protected FreedSegment prev;
        
        public FreedSegment(int offset, int length) {
            this.offset = offset;
            this.length = length;
        }
        
        protected int getEnd() {
            return this.offset + this.length;
        }
        
        protected void mergeInto(FreedSegment entry) {
            this.length = this.length + entry.length;
            this.next = entry.next;
            
            if (this.next != null) {
                this.next.prev = this;
            }
        }
    }
    
    
}
