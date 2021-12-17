package me.jellysquid.mods.sodium.client.gl.arena;

import it.unimi.dsi.fastutil.objects.Reference2ObjectMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import me.jellysquid.mods.sodium.client.gl.buffer.GlBuffer;
import me.jellysquid.mods.sodium.client.gl.buffer.GlBufferUsage;
import me.jellysquid.mods.sodium.client.gl.buffer.GlMutableBuffer;
import me.jellysquid.mods.sodium.client.gl.device.CommandList;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.util.Map;

public class SwapBufferArena implements GlBufferArena {
    private final Reference2ObjectMap<GlBufferSegment, StashedData> active = new Reference2ObjectOpenHashMap<>();

    private final GlMutableBuffer deviceBuffer;
    private final GlMutableBuffer stagingBuffer;

    private int used;

    public SwapBufferArena(CommandList commandList) {
        this.deviceBuffer = commandList.createMutableBuffer();
        this.stagingBuffer = commandList.createMutableBuffer();
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
    public void delete(CommandList commands) {
        for (StashedData data : this.active.values()) {
            data.destroy();
        }

        this.active.clear();

        commands.deleteBuffer(this.deviceBuffer);
        commands.deleteBuffer(this.stagingBuffer);
    }

    @Override
    public boolean isEmpty() {
        return this.used == 0;
    }

    @Override
    public GlBuffer getBufferObject() {
        return this.deviceBuffer;
    }

    @Override
    public GlBufferSegment upload(CommandList commandList, ByteBuffer buf) {
        int totalBytes = this.active.values().stream().mapToInt(i -> i.size).sum() + buf.remaining();

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

        GlBufferSegment seg = new GlBufferSegment(this, writePointer, buf.remaining());

        this.used += seg.getLength();

        MemoryUtil.memCopy(
                MemoryUtil.memAddress(buf),
                MemoryUtil.memAddress(buffer, seg.getOffset()),
                seg.getLength()
        );

        StashedData stashedData = new StashedData(buf);
        this.active.put(seg, stashedData);

        commandList.uploadData(this.stagingBuffer, buffer, GlBufferUsage.STREAM_COPY);
        commandList.allocateStorage(this.deviceBuffer, buffer.remaining(), GlBufferUsage.STATIC_DRAW);
        commandList.copyBufferSubData(this.stagingBuffer, this.deviceBuffer, 0, 0, buffer.remaining());
        commandList.allocateStorage(this.stagingBuffer, 0L, GlBufferUsage.STREAM_COPY);

        MemoryUtil.memFree(buffer);

        return seg;
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
