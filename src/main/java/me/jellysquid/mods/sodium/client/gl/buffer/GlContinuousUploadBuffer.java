package me.jellysquid.mods.sodium.client.gl.buffer;

import me.jellysquid.mods.sodium.client.SodiumClientMod;
import me.jellysquid.mods.sodium.client.gl.arena.staging.FallbackStagingBuffer;
import me.jellysquid.mods.sodium.client.gl.arena.staging.MappedStagingBuffer;
import me.jellysquid.mods.sodium.client.gl.arena.staging.StagingBuffer;
import me.jellysquid.mods.sodium.client.gl.device.CommandList;
import me.jellysquid.mods.sodium.client.gl.device.RenderDevice;

import java.nio.ByteBuffer;

public class GlContinuousUploadBuffer extends GlMutableBuffer {
    private static final GlBufferUsage BUFFER_USAGE = GlBufferUsage.STATIC_DRAW;
    private static final int DEFAULT_INITIAL_CAPACITY = 1024;

    private final StagingBuffer stagingBuffer;

    private int capacity;

    public GlContinuousUploadBuffer(CommandList commands, int initialCapacity) {
        super();
        this.capacity = initialCapacity;
        this.stagingBuffer = createStagingBuffer(commands);
        commands.allocateStorage(this, this.capacity, BUFFER_USAGE);
    }

    public GlContinuousUploadBuffer(CommandList commands) {
        this(commands, DEFAULT_INITIAL_CAPACITY);
    }

    public void uploadOverwrite(CommandList commandList, ByteBuffer data, int size) {
        ensureCapacity(commandList, size);
        this.stagingBuffer.enqueueCopy(commandList, data, this, 0);
        this.stagingBuffer.flush(commandList);
    }

    public void ensureCapacity(CommandList commandList, int capacity) {
        if (capacity > this.capacity) {
            this.capacity = capacity;
            commandList.allocateStorage(this, capacity, BUFFER_USAGE);
        }
    }

    private static StagingBuffer createStagingBuffer(CommandList commandList) {
        if (SodiumClientMod.options().advanced.useAdvancedStagingBuffers && MappedStagingBuffer.isSupported(RenderDevice.INSTANCE)) {
            return new MappedStagingBuffer(commandList);
        }

        return new FallbackStagingBuffer(commandList);
    }
}
