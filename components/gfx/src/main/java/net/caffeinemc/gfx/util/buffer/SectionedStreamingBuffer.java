package net.caffeinemc.gfx.util.buffer;

import java.nio.ByteBuffer;
import java.util.EnumSet;
import java.util.Set;
import net.caffeinemc.gfx.api.buffer.Buffer;
import net.caffeinemc.gfx.api.buffer.MappedBuffer;
import net.caffeinemc.gfx.api.buffer.MappedBufferFlags;
import net.caffeinemc.gfx.api.device.RenderDevice;
import net.caffeinemc.gfx.internal.MathUtil;
import org.lwjgl.system.MemoryUtil;

// TODO: convert to all longs
public class SectionedStreamingBuffer implements StreamingBuffer {
    protected final RenderDevice device;
    private final Set<MappedBufferFlags> bufferFlags;
    private final int sectionCount;
    private final int alignment;

    private final WritableSection[] sections;
    protected MappedBuffer buffer;
    protected int sectionCapacity;
    private int alignedStride;

    private int lastFrameIdx = -1;

    public SectionedStreamingBuffer(RenderDevice device, int alignment, int sectionCapacity, int sectionCount, Set<MappedBufferFlags> extraFlags) {
        this.device = device;
        this.sectionCount = sectionCount;
        this.alignment = alignment;
        this.bufferFlags = EnumSet.of(MappedBufferFlags.WRITE);
        this.bufferFlags.addAll(extraFlags);

        this.sectionCapacity = sectionCapacity;
        this.alignedStride = MathUtil.align(sectionCapacity, alignment);
        this.buffer = this.device.createMappedBuffer((long) this.alignedStride * sectionCount, this.bufferFlags);
        this.sections = new WritableSection[sectionCount];
        this.updateSections();
    }

    /**
     * WARNING: RESIZING PERSISTENT BUFFERS IS *SUPER SLOW*!!!!
     * ONLY USE THIS METHOD IF *ABSOLUTELY NECESSARY*!!!!
     *
     * This method will also invalidate any existing WritableSections,
     * so make sure none are in use before this is called.
     *
     * @return true if the buffer was resized
     */
    public boolean resizeIfNeeded(WritableSection currentSection, int sectionCapacity, boolean copyContents) {
        // TODO: add path for if sectioncapacity is still smaller than alignedstride
        if (sectionCapacity > this.sectionCapacity) {
            int newSectionCapacity = Math.max(sectionCapacity, this.sectionCapacity * 2);
            int newAlignedStride = MathUtil.align(newSectionCapacity, this.alignment);
            long newBufferCapacity = (long) newAlignedStride * this.sectionCount;

            // flush pending data for current section, previous sections should already be flushed
            currentSection.flushPartial();

            MappedBuffer newBuffer;
            // create new buffer
            if (copyContents) {
                // copy old contents to new buffer before mapping
                newBuffer = this.device.createMappedBuffer(
                        newBufferCapacity,
                        buffer -> {
                            for (int idx = 0; idx < this.sectionCount; idx++) {
                                int oldSectionStart = this.alignedStride * idx;
                                int newSectionStart = newAlignedStride * idx;
                                this.device.copyBuffer(
                                        this.buffer,
                                        buffer,
                                        oldSectionStart,
                                        newSectionStart,
                                        this.sectionCapacity
                                );
                            }
                        },
                        this.bufferFlags
                );
            } else {
                newBuffer = this.device.createMappedBuffer(newBufferCapacity, this.bufferFlags);
            }

            // delete old buffer
            this.device.deleteBuffer(this.buffer);

            this.sectionCapacity = newSectionCapacity;
            this.alignedStride = newAlignedStride;
            this.buffer = newBuffer;

            this.updateSections();
            return true;
        }
        return false;
    }

    private void updateSections() {
        for (int idx = 0; idx < this.sectionCount; idx++) {
            WritableSection prevSection = this.sections[idx];
    
            int start = this.alignedStride * idx;
            ByteBuffer view = MemoryUtil.memSlice(this.buffer.view(), start, this.sectionCapacity);
            // warning: only works when updates are larger than prior
            if (prevSection != null) {
                view.position(prevSection.getView().position());
            }

            this.sections[idx] = this.createSection(view, start);
        }
    }
    
    private WritableSection getSectionUnaligned(int frameIndex) {
        int sectionIdx = frameIndex % this.sectionCount;
    
        // not good, but whatever
        if (frameIndex != this.lastFrameIdx) {
            this.sections[sectionIdx].reset();
            this.lastFrameIdx = frameIndex;
        }
    
       return this.sections[sectionIdx];
    }

    /**
     * Obtains an aligned, mapped section of the streaming buffer which can be written to.
     */
    @Override
    public WritableSection getSection(int frameIndex) {
        WritableSection section = this.getSectionUnaligned(frameIndex);

        ByteBuffer view = section.getView();
        int alignedPosition = MathUtil.align(view.position(), this.alignment);
        view.position(alignedPosition);
        return section;
    }

    /**
     * Obtains a mapped section of the streaming buffer which can be written to.
     * If needed, this buffer will be resized to fit the extra size needed.
     */
    @Override
    public WritableSection getSection(int frameIndex, int extraSize, boolean copyContents) {
        WritableSection section = this.getSectionUnaligned(frameIndex);

        // resize if needed
        ByteBuffer view = section.getView();
        int alignedPosition = MathUtil.align(view.position(), this.alignment);
        int requiredSize = alignedPosition + extraSize;
        boolean resized = this.resizeIfNeeded(section, requiredSize, copyContents);

        // need to account for if sections were updated
        if (resized) {
            section = this.getSection(frameIndex);
        } else {
            view.position(alignedPosition);
        }

        return section;
    }

    @Override
    public Buffer getBufferObject() {
        return this.buffer;
    }

    @Override
    public int getAlignment() {
        return this.alignment;
    }

    @Override
    public long getDeviceUsedMemory() {
        long deviceUsed = 0;
        for (WritableSection section : this.sections) {
            deviceUsed += section.getView().position();
        }
        return deviceUsed;
    }

    @Override
    public long getDeviceAllocatedMemory() {
        return (long) this.alignedStride * this.sectionCount;
    }

    @Override
    public void delete() {
        this.device.deleteBuffer(this.buffer);
    }

    // FIXME: this is gross
    protected WritableSection createSection(ByteBuffer view, long offset) {
        return new SectionImpl(this.buffer, view, offset);
    }


    protected static class SectionImpl implements StreamingBuffer.WritableSection {
        // we hold onto the buffer so if the parent's gets changed, this won't update.
        // this causes an error to happen if we try to flush after the buffer has been deleted already.
        protected final MappedBuffer buffer;
        protected final ByteBuffer view;

        protected final long offset;

        protected int lastFlushEndPos;

        public SectionImpl(MappedBuffer buffer, ByteBuffer view, long offset) {
            this.buffer = buffer;
            this.view = view;
            this.offset = offset;
        }

        public void reset() {
            this.view.position(0);
            this.lastFlushEndPos = 0;
        }

        public void flushPartial() {
            long length = this.view.position() - this.lastFlushEndPos;
            if (length > 0) {
                this.buffer.flush(this.offset + this.lastFlushEndPos, length);
                this.lastFlushEndPos += length;
            }
        }

        public void flushFull() {
            this.buffer.flush(this.offset, this.view.capacity());
        }

        public ByteBuffer getView() {
            return this.view;
        }

        public long getDeviceOffset() {
            return this.offset;
        }

    }

}
