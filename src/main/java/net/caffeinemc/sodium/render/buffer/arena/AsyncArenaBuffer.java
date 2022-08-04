package net.caffeinemc.sodium.render.buffer.arena;

import it.unimi.dsi.fastutil.longs.LongBidirectionalIterator;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongRBTreeSet;
import it.unimi.dsi.fastutil.longs.LongSortedSet;
import java.util.LinkedList;
import java.util.List;
import javax.annotation.Nullable;
import net.caffeinemc.gfx.api.buffer.Buffer;
import net.caffeinemc.gfx.api.buffer.ImmutableBuffer;
import net.caffeinemc.gfx.api.device.RenderDevice;
import net.caffeinemc.gfx.util.buffer.BufferPool;
import net.caffeinemc.gfx.util.buffer.streaming.StreamingBuffer;
import org.apache.commons.lang3.Validate;
import org.lwjgl.system.MemoryUtil;

// TODO: handle alignment
// TODO: handle element vs pointers
public class AsyncArenaBuffer implements ArenaBuffer {
    private static final boolean CHECK_ASSERTIONS = true;

    private final float resizeMultiplier;

    private final RenderDevice device;
    private final StreamingBuffer stagingBuffer;
    private final BufferPool<ImmutableBuffer> bufferPool;
    
    private ImmutableBuffer deviceBuffer;

    private final LongRBTreeSet freedSegmentsByOffset = new LongRBTreeSet(BufferSegment::compareOffset);
    private final LongRBTreeSet freedSegmentsByLength = new LongRBTreeSet(BufferSegment::compareLengthOffset);

    private int capacity;
    private int position;
    private int used;

    private final int stride;

    public AsyncArenaBuffer(
            RenderDevice device,
            StreamingBuffer stagingBuffer,
            BufferPool<ImmutableBuffer> bufferPool,
            int initialCapacityTarget,
            float resizeMultiplier,
            int stride
    ) {
        this.device = device;
        this.stagingBuffer = stagingBuffer;
        this.bufferPool = bufferPool;
        this.resizeMultiplier = resizeMultiplier;
        this.stride = stride;
        this.setDeviceBuffer(bufferPool.getBufferLenient(this.toBytes(initialCapacityTarget)));
    }
    
    public void reset() {
        this.freedSegmentsByOffset.clear();
        this.freedSegmentsByLength.clear();
        this.used = 0;
        this.position = 0;
    }

    private void grow(int capacityTarget) {
        if (this.capacity >= capacityTarget) {
            throw new UnsupportedOperationException("New capacity must be larger than old capacity");
        }

        this.checkAssertions();
    
        ImmutableBuffer srcBufferObj = this.deviceBuffer;
        ImmutableBuffer dstBufferObj = this.bufferPool.getBufferLenient(this.toBytes(capacityTarget));
        
        this.device.copyBuffer(
                srcBufferObj,
                dstBufferObj,
                0,
                0,
                this.toBytes(this.position)
        );
        
        this.bufferPool.recycleBuffer(srcBufferObj);
    
        this.setDeviceBuffer(dstBufferObj);

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
        long result = BufferSegment.INVALID;
    
        // this is used to get the closest in the tree
        long tempKey = BufferSegment.createKey(size, 0);
        LongIterator itr = this.freedSegmentsByLength.iterator(tempKey);
        
        if (itr.hasNext()) {
            long freeSegment = itr.nextLong();
            // the segment will always need to be removed from here
            this.freedSegmentsByLength.remove(freeSegment);
            this.freedSegmentsByOffset.remove(freeSegment);
            
            if (BufferSegment.getLength(freeSegment) == size) {
                // no need to add back to tree
                result = freeSegment;
            } else {
                result = BufferSegment.createKey(
                        size,
                        BufferSegment.getEnd(freeSegment) - size
                );
                
                long newFreeSegment = BufferSegment.createKey(
                        BufferSegment.getLength(freeSegment) - size,
                        BufferSegment.getOffset(freeSegment)
                );
                
                this.freedSegmentsByLength.add(newFreeSegment);
                this.freedSegmentsByOffset.add(newFreeSegment);
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

    @Override
    public void free(long key) {
        this.used -= BufferSegment.getLength(key);
        
        LongBidirectionalIterator itr = this.freedSegmentsByOffset.iterator(key);
        
        // get and attempt to merge with left key
        if (itr.hasPrevious()) {
            long prev = itr.previousLong();
            
            if (BufferSegment.getEnd(prev) == BufferSegment.getOffset(key)) {
                itr.remove();
                this.freedSegmentsByLength.remove(prev);
        
                // merge key
                key = BufferSegment.createKey(
                        BufferSegment.getLength(prev) + BufferSegment.getLength(key),
                        BufferSegment.getOffset(prev)
                );
                
            } else {
                // need to skip one in the iterator to cancel out the previous() call
                itr.nextLong();
            }
        }
    
        // fast path if we happen to be on the far right, touching the bump allocator position
        // this is checked after we merge with the left key to guarantee that there won't be a freed segment
        // touching the bump allocator position.
        if (BufferSegment.getEnd(key) == this.position) {
            this.position -= BufferSegment.getLength(key);
    
            this.checkAssertions();
            return;
        }
        
        // get and attempt to merge with right key
        if (itr.hasNext()) {
            long next = itr.nextLong();
            
            if (BufferSegment.getEnd(key) == BufferSegment.getOffset(next)) {
                itr.remove();
                this.freedSegmentsByLength.remove(next);
        
                // merge key
                key = BufferSegment.createKey(
                        BufferSegment.getLength(key) + BufferSegment.getLength(next),
                        BufferSegment.getOffset(key)
                );
            }
        }
        
        this.freedSegmentsByOffset.add(key);
        this.freedSegmentsByLength.add(key);
    
        this.checkAssertions();
    }
    
    @Nullable
    public LongSortedSet compact() {
        if (this.freedSegmentsByOffset.isEmpty()) {
            return null;
        }

        this.checkAssertions();

        var srcBufferObj = this.deviceBuffer;
        var dstBufferObj = this.bufferPool.getBufferLenient(this.toBytes(this.used));

        long dstOffset = 0;
        // the end of the previous freed segment (exclusive) is also the start of the next allocated segment (inclusive)
        long prevFreedSegmentEnd = 0;

        for (long freedSegment : this.freedSegmentsByOffset) {
            long freedOffset = this.toBytes(BufferSegment.getOffset(freedSegment));
            long freedLength = this.toBytes(BufferSegment.getLength(freedSegment));
            long copyLength = freedOffset - prevFreedSegmentEnd;

            // if all freed segments are merged correctly, then this should only be able to be false on the first
            // segment
            if(copyLength != 0) {
                this.device.copyBuffer(
                        srcBufferObj,
                        dstBufferObj,
                        prevFreedSegmentEnd,
                        dstOffset,
                        copyLength
                );

                dstOffset += copyLength;
            }

            prevFreedSegmentEnd = freedOffset + freedLength;
        }
        
        // because free guarantees that we will never have a segment touching the bump position, there will always be
        // an allocated segment after the last freed segment.
        this.device.copyBuffer(
                srcBufferObj,
                dstBufferObj,
                prevFreedSegmentEnd,
                dstOffset,
                this.toBytes(this.position) - prevFreedSegmentEnd
        );
    
        this.bufferPool.recycleBuffer(srcBufferObj);
        this.setDeviceBuffer(dstBufferObj);
        this.position = this.used;
    
        // TODO: check if this is expensive
        // if it is, just provide an immutable view and hope the caller doesn't hold on to it for too long
        LongSortedSet removedSegmentsByOffset = (LongRBTreeSet) this.freedSegmentsByOffset.clone();
    
        this.freedSegmentsByOffset.clear();
        this.freedSegmentsByLength.clear();

        this.checkAssertions();

        return removedSegmentsByOffset;
    }
    
    @Override
    public float getFragmentation() {
        // original algorithm
//        return 1.0f - ((float) this.used / this.position);
    
        // yoinked from here: https://stackoverflow.com/a/4587077/4563900
//        try {
//            int largestFreedLength = BufferSegment.getLength(this.freedSegmentsByLength.lastLong());
//            int free = this.capacity - this.used;
//            return (float) (free - largestFreedLength) / free;
//        } catch (NoSuchElementException ignored) {
//            // no freed segments, fragmentation is 0%
//            return 0.0f;
//        }
    
        // a buffer with alternating length 1 free and length 1 alloc segments should have a 100% fragmentation rate.
        // a buffer with alternating length 2 free and length 2 alloc segments should have a 50% fragmentation rate.
        long freeBytes = this.toBytes(this.position - this.used);
        long totalBytes = this.toBytes(this.position);
        double segmentRatio = (double) this.freedSegmentsByOffset.size() / freeBytes;
        double sizeRatio = (double) (freeBytes * 2) / totalBytes;
        return (float) (segmentRatio * sizeRatio);
    }
    
    @Override
    public void delete() {
        this.bufferPool.recycleBuffer(this.deviceBuffer);
    }

    @Override
    public boolean isEmpty() {
        return this.used <= 0;
    }

    @Override
    public Buffer getBufferObject() {
        return this.deviceBuffer;
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
                            upload.bufferSegmentResult,
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
        long segment = this.alloc(this.toElements(transfer.length()));

        if (segment == BufferSegment.INVALID) {
            return false;
        }

        // Copy the uploads from the streaming buffer to the arena buffer
        this.device.copyBuffer(
                streamingBuffer,
                this.deviceBuffer,
                transfer.offset(),
                this.toBytes(BufferSegment.getOffset(segment)),
                transfer.length()
        );

        transfer.bufferSegmentHolder().set(segment);

        return true;
    }

    public void ensureCapacity(int elementCount) {
        // Re-sizing the arena no longer results in a compaction, so we need to go from the current pointer
        // to make sure we have enough capacity. Any extra caused by allocations filling freed areas is just
        // a bonus :tiny_potato:.
        int elementsNeeded = elementCount - (this.capacity - this.position);

        // Try to allocate some extra buffer space unless this is an unusually large allocation
        this.grow(Math.max(this.capacity + (int) (this.capacity * this.resizeMultiplier), this.capacity + elementsNeeded));
    }
    
    private void setDeviceBuffer(ImmutableBuffer buffer) {
        this.deviceBuffer = buffer;
        this.capacity = this.toElements(buffer.capacity());
    }

    private void checkAssertions() {
        if (CHECK_ASSERTIONS) {
            this.checkAssertions0();
        }
    }

    private void checkAssertions0() {
        int used = 0;
        
        long prev = BufferSegment.INVALID;
        
        for(long freedSegment : this.freedSegmentsByOffset) {
            int offset = BufferSegment.getOffset(freedSegment);
            int length = BufferSegment.getLength(freedSegment);
            int end = BufferSegment.getEnd(freedSegment);
            
            Validate.isTrue(offset >= 0, "segment.offset < 0: out of bounds");
            Validate.isTrue(end != this.position, "segment.end == arena.position: segment touches bump pos (failure to track)");
            Validate.isTrue(end < this.position, "segment.end > arena.position: out of bounds");
            
            used += length;
            
            if (prev != BufferSegment.INVALID) {
                int prevEnd = BufferSegment.getEnd(prev);
    
                Validate.isTrue(prevEnd != offset,
                                "segment.prev.end == segment.offset: failure to merge");
                Validate.isTrue(prevEnd < offset,
                                "segment.prev.end > segment.offset: overlapping segments (corrupted)");
            }
            
            prev = freedSegment;
        }
    
        Validate.isTrue(this.used >= 0, "arena.used < 0: failure to track");
        Validate.isTrue(this.position <= this.capacity,
                        "arena.position > arena.capacity: failure to track");
        Validate.isTrue(this.used <= this.position, "arena.used > arena.position: failure to track");
        Validate.isTrue(this.position - this.used == used, "arena.used is invalid");
        Validate.isTrue(this.freedSegmentsByLength.size() == this.freedSegmentsByOffset.size(),
                        "freedSegmentsByLength.size != freedSegmentsByOffset.size, mismatched add/remove");
        Validate.isTrue(this.deviceBuffer.capacity() == this.toBytes(this.capacity),
                        "this.capacity != buffer.capacity, failure to track");
    }

    private long toBytes(int index) {
        return (long) index * this.stride;
    }

    private int toElements(long bytes) {
        return (int) (bytes / this.stride);
    }

    @Override
    public int getStride() {
        return this.stride;
    }
    
}
