package me.jellysquid.mods.sodium.client.gl.arena.staging;

import me.jellysquid.mods.sodium.client.gl.buffer.*;
import me.jellysquid.mods.sodium.client.gl.device.CommandList;
import me.jellysquid.mods.sodium.client.gl.util.EnumBitField;
import org.lwjgl.system.MemoryStack;

public class StagingBufferBuilder {
    private static final EnumBitField<GlBufferStorageFlags> STORAGE_FLAGS =
            EnumBitField.of(GlBufferStorageFlags.PERSISTENT, GlBufferStorageFlags.CLIENT_STORAGE, GlBufferStorageFlags.MAP_WRITE);

    private static final EnumBitField<GlBufferMapFlags> MAP_FLAGS =
            EnumBitField.of(GlBufferMapFlags.PERSISTENT, GlBufferMapFlags.INVALIDATE_BUFFER, GlBufferMapFlags.WRITE, GlBufferMapFlags.EXPLICIT_FLUSH);

    private final MappedBuffer mappedBuffer;

    private int start = 0;
    private int pos = 0;

    private int numUploads = 0;

    public StagingBufferBuilder(CommandList commandList, int capacity) {
        GlImmutableBuffer buffer = commandList.createImmutableBuffer(capacity, STORAGE_FLAGS);
        GlBufferMapping map = commandList.mapBuffer(buffer, 0, capacity, MAP_FLAGS);

        this.mappedBuffer = new MappedBuffer(buffer, map);
    }

    public void push(MemoryStack stack, long ptr, int size) {
        this.mappedBuffer.map.write(stack, ptr, size, pos);
        pos += size;
    }

    public void endAndUpload(CommandList commandList, GlBuffer dst, long writeOffset) {
        this.flush(commandList, dst, this.start, this.pos - this.start, writeOffset);

        this.numUploads += 1;
        if (numUploads >= 3 /* TODO: Replace with render-ahead limit */) {
            this.reset();
        }

        this.start = this.pos;
    }

    private void reset() {
        this.start = 0;
        this.pos = 0;
        this.numUploads = 0;
    }

    private void flush(CommandList commandList, GlBuffer dst, int readOffset, int length, long writeOffset) {
        commandList.flushMappedRange(this.mappedBuffer.map, readOffset, length);
        commandList.copyBufferSubData(this.mappedBuffer.buffer, dst, readOffset, writeOffset, length);
    }

    private record MappedBuffer(GlImmutableBuffer buffer,
                                GlBufferMapping map) {
        public void delete(CommandList commandList) {
            commandList.unmap(this.map);
            commandList.deleteBuffer(this.buffer);
        }
    }
}
