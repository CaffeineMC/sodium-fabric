package net.caffeinemc.sodium.render.buffer;

import net.caffeinemc.gfx.api.buffer.Buffer;
import net.caffeinemc.gfx.api.buffer.MappedBuffer;
import net.caffeinemc.gfx.api.buffer.MappedBufferFlags;
import net.caffeinemc.gfx.api.device.RenderDevice;
import net.caffeinemc.sodium.util.MathUtil;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.EnumSet;

// TODO: convert to all longs
public class StreamingBuffer {
    private final RenderDevice device;
    private final EnumSet<MappedBufferFlags> bufferFlags;
    private final int sectionCount;
    private final int alignment;

    private final WritableSection[] sections;
    private MappedBuffer buffer;
    private int sectionCapacity;
    private int alignedStride;

    private int lastFrameIdx = -1;

    public StreamingBuffer(RenderDevice device, int alignment, int sectionCapacity, int sectionCount, MappedBufferFlags... extraFlags) {
        this.device = device;
        this.sectionCount = sectionCount;
        this.alignment = alignment;
        this.bufferFlags = EnumSet.of(MappedBufferFlags.WRITE);
        Collections.addAll(this.bufferFlags, extraFlags);

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
     * This method will also invalidate any existing Slices, so
     * make sure none are in use before this is called.
     *
     * @return true if the buffer was resized;
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
            int start = this.alignedStride * idx;
            var view = MemoryUtil.memSlice(this.buffer.view(), start, this.sectionCapacity);

            this.sections[idx] = new WritableSection(this.buffer, view, start);
        }
    }

    /**
     * Obtains a mapped section of the streaming buffer which can be written to.
     */
    public WritableSection getSection(int frameIndex) {
        int sectionIdx = frameIndex % this.sectionCount;

        // not good, but whatever
        if (frameIndex != this.lastFrameIdx) {
            this.sections[sectionIdx].reset();
            this.lastFrameIdx = frameIndex;
        }

        return this.sections[sectionIdx];
    }

    /**
     * Obtains a mapped section of the streaming buffer which can be written to.
     * If needed, this buffer will be resized to fit the extra size needed.
     */
    public WritableSection getSectionWithSize(int frameIndex, int extraSize, boolean copyContents) {
        StreamingBuffer.WritableSection section = this.getSection(frameIndex);

        // resize if needed
        int requiredSize = section.getView().position() + extraSize;
        boolean resized = this.resizeIfNeeded(section, requiredSize, copyContents);

        // need to account for if sections were updated
        if (resized) {
            section = this.getSection(frameIndex);
        }

        return section;
    }

    public Buffer getBuffer() {
        return this.buffer;
    }

    public int getAlignment() {
        return this.alignment;
    }

    public void delete() {
        this.device.deleteBuffer(this.buffer);
    }

    public static final class WritableSection {
        private final MappedBuffer buffer;
        private final ByteBuffer view;

        private final long offset;

        private int lastFlushEndPos;

        public WritableSection(MappedBuffer buffer, ByteBuffer view, long offset) {
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

        public MappedBuffer getBuffer() {
            return this.buffer;
        }

        public ByteBuffer getView() {
            return this.view;
        }

        public long getOffset() {
            return this.offset;
        }

    }

}
