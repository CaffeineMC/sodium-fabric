package me.jellysquid.mods.sodium.render.chunk.arena;

import it.unimi.dsi.fastutil.objects.Reference2ObjectMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import me.jellysquid.mods.thingl.buffer.Buffer;
import me.jellysquid.mods.thingl.buffer.BufferUsage;
import me.jellysquid.mods.thingl.buffer.MutableBuffer;
import me.jellysquid.mods.thingl.buffer.MutableBufferImpl;
import me.jellysquid.mods.thingl.device.RenderDevice;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SwapBufferArena implements GlBufferArena {
    private final Reference2ObjectMap<GlBufferSegment, StashedData> active = new Reference2ObjectOpenHashMap<>();

    private final RenderDevice device;
    private final MutableBuffer deviceBuffer;
    private final MutableBuffer stagingBuffer;

    private int used;

    public SwapBufferArena(RenderDevice device) {
        this.device = device;
        this.deviceBuffer = device.createMutableBuffer();
        this.stagingBuffer = device.createMutableBuffer();
    }

    @Override
    public int getDeviceUsedMemory() {
        return this.used;
    }

    @Override
    public int getDeviceAllocatedMemory() {
        return this.used;
    }

    @Override
    public void free(GlBufferSegment entry) {
        StashedData data = this.active.remove(entry);

        if (data == null) {
            throw new IllegalStateException("Double-free");
        }

        this.used -= entry.getLength();

        data.destroy();
    }

    @Override
    public void delete() {
        for (StashedData data : this.active.values()) {
            data.destroy();
        }

        this.active.clear();

        this.device.deleteBuffer(this.deviceBuffer);
        this.device.deleteBuffer(this.stagingBuffer);
    }

    @Override
    public boolean isEmpty() {
        return this.used == 0;
    }

    @Override
    public Buffer getBufferObject() {
        return this.deviceBuffer;
    }

    @Override
    public boolean upload(Stream<PendingUpload> stream) {
        List<PendingUpload> uploads = stream.collect(Collectors.toList());

        int totalBytes = uploads.stream().mapToInt(PendingUpload::getLength).sum() +
                this.active.values().stream().mapToInt(i -> i.size).sum();

        ByteBuffer buffer = MemoryUtil.memAlloc(totalBytes);

        int writePointer = 0;

        if (!this.active.isEmpty()) {
            for (Map.Entry<GlBufferSegment, StashedData> entry : this.active.entrySet()) {
                GlBufferSegment seg = entry.getKey();
                StashedData data = entry.getValue();

                MemoryUtil.memCopy(
                        MemoryUtil.memAddress(data.buf),
                        MemoryUtil.memAddress(buffer, writePointer),
                        data.size);

                seg.setOffset(writePointer);
                writePointer += seg.getLength();
            }
        }

        for (PendingUpload upload : uploads) {
            ByteBuffer payload = upload.getDataBuffer().getDirectBuffer();

            GlBufferSegment seg = new GlBufferSegment(this, writePointer, payload.remaining());
            upload.setResult(seg);

            this.used += seg.getLength();

            MemoryUtil.memCopy(
                    MemoryUtil.memAddress(payload),
                    MemoryUtil.memAddress(buffer, seg.getOffset()),
                    seg.getLength()
            );

            writePointer += seg.getLength();

            StashedData stashedData = new StashedData(payload);
            this.active.put(seg, stashedData);
        }

        this.device.uploadData(this.stagingBuffer, buffer, BufferUsage.STREAM_COPY);
        this.device.allocateStorage(this.deviceBuffer, buffer.remaining(), BufferUsage.STATIC_DRAW);
        this.device.copyBufferSubData(this.stagingBuffer, this.deviceBuffer, 0, 0, buffer.remaining());
        this.device.allocateStorage(this.stagingBuffer, 0L, BufferUsage.STREAM_COPY);

        MemoryUtil.memFree(buffer);

        return true;
    }

    private static class StashedData {
        public ByteBuffer buf;
        public int size;

        public StashedData(ByteBuffer src) {
            ByteBuffer copy = MemoryUtil.memAlloc(src.remaining());
            MemoryUtil.memCopy(src, copy);

            this.size = copy.remaining();
            this.buf = copy;
        }

        public void destroy() {
            MemoryUtil.memFree(this.buf);
        }
    }
}
