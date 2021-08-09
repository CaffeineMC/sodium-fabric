package me.jellysquid.mods.sodium.client.gl.arena.staging;

import it.unimi.dsi.fastutil.PriorityQueue;
import it.unimi.dsi.fastutil.objects.ObjectArrayFIFOQueue;
import me.jellysquid.mods.sodium.client.SodiumClientMod;
import me.jellysquid.mods.sodium.client.gl.buffer.*;
import me.jellysquid.mods.sodium.client.gl.device.CommandList;
import me.jellysquid.mods.sodium.client.gl.device.RenderDevice;
import me.jellysquid.mods.sodium.client.gl.functions.BufferStorageFunctions;
import me.jellysquid.mods.sodium.client.gl.util.EnumBitField;

import java.nio.ByteBuffer;
import java.util.Arrays;

public class MappedStagingBuffer implements StagingBuffer {
    private static final int BUFFER_SIZE = 1024 * 1024 * 16; // 16 MB

    private static final EnumBitField<GlBufferStorageFlags> STORAGE_FLAGS =
            EnumBitField.of(GlBufferStorageFlags.PERSISTENT, GlBufferStorageFlags.CLIENT_STORAGE, GlBufferStorageFlags.MAP_WRITE);

    private static final EnumBitField<GlBufferMapFlags> MAP_FLAGS =
            EnumBitField.of(GlBufferMapFlags.PERSISTENT, GlBufferMapFlags.INVALIDATE_BUFFER, GlBufferMapFlags.WRITE, GlBufferMapFlags.EXPLICIT_FLUSH);

    private final FallbackStagingBuffer fallbackStagingBuffer;

    private final MappedBuffer[] buffers = new MappedBuffer[SodiumClientMod.options().advanced.maxPreRenderedFrames + 1];

    private final PriorityQueue<CopyCommand> pendingCopies = new ObjectArrayFIFOQueue<>();

    private int writeOffset;

    private MappedBuffer currentBuffer;
    private int currentBufferIdx;

    public MappedStagingBuffer(CommandList commandList) {
        for (int i = 0; i < this.buffers.length; i++) {
            GlImmutableBuffer buffer = commandList.createImmutableBuffer(BUFFER_SIZE, STORAGE_FLAGS);
            GlBufferMapping map = commandList.mapBuffer(buffer, 0, BUFFER_SIZE, MAP_FLAGS);

            this.buffers[i] = new MappedBuffer(buffer, map);
        }

        this.fallbackStagingBuffer = new FallbackStagingBuffer(commandList);
        this.currentBuffer = this.buffers[this.currentBufferIdx];
    }

    public static boolean isSupported(RenderDevice instance) {
        return instance.getDeviceFunctions().getBufferStorageFunctions() != BufferStorageFunctions.NONE;
    }

    @Override
    public void enqueueCopy(CommandList commandList, ByteBuffer data, GlBuffer dst, long writeOffset) {
        if (this.writeOffset + data.remaining() > BUFFER_SIZE) {
            this.fallbackStagingBuffer.enqueueCopy(commandList, data, dst, writeOffset);
            return;
        }

        int offset = this.writeOffset;

        this.currentBuffer.getMap()
                .write(data, offset);
        this.writeOffset += data.remaining();

        this.pendingCopies.enqueue(new CopyCommand(dst, offset, writeOffset, data.remaining()));
    }

    @Override
    public void flush(CommandList commandList) {
        commandList.flushMappedRange(this.currentBuffer.getMap(), 0, this.writeOffset);

        while (!this.pendingCopies.isEmpty()) {
            CopyCommand command = this.pendingCopies.dequeue();
            commandList.copyBufferSubData(this.currentBuffer.getBufferObject(), command.buffer, command.readOffset, command.writeOffset, command.bytes);
        }
    }

    @Override
    public void delete(CommandList commandList) {
        for (MappedBuffer buffer : this.buffers) {
            buffer.delete(commandList);
        }

        Arrays.fill(this.buffers, null);

        this.fallbackStagingBuffer.delete(commandList);
        this.pendingCopies.clear();
    }

    @Override
    public void flip() {
        this.currentBufferIdx = (this.currentBufferIdx + 1) % this.buffers.length;
        this.currentBuffer = this.buffers[this.currentBufferIdx];
        this.writeOffset = 0;
    }

    private record CopyCommand(GlBuffer buffer, long readOffset, long writeOffset, long bytes) {
    }

    private record MappedBuffer(GlImmutableBuffer buffer,
                                GlBufferMapping map) {
        public void delete(CommandList commandList) {
            commandList.unmap(this.map);
            commandList.deleteBuffer(this.buffer);
        }

        public GlBufferMapping getMap() {
            return this.map;
        }

        public GlBuffer getBufferObject() {
            return this.buffer;
        }
    }
}
