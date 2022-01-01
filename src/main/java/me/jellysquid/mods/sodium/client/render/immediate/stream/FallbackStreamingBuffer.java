package me.jellysquid.mods.sodium.client.render.immediate.stream;

import me.jellysquid.mods.sodium.client.gl.buffer.GlBuffer;
import me.jellysquid.mods.sodium.client.gl.buffer.GlBufferUsage;
import me.jellysquid.mods.sodium.client.gl.buffer.GlMutableBuffer;
import me.jellysquid.mods.sodium.client.gl.device.CommandList;
import me.jellysquid.mods.sodium.client.gl.sync.GlFence;

import java.nio.ByteBuffer;
import java.util.function.Supplier;

public class FallbackStreamingBuffer implements StreamingBuffer {
    private final GlMutableBuffer buffer;

    public FallbackStreamingBuffer(CommandList commandList) {
        this.buffer = commandList.createMutableBuffer();
    }

    @Override
    public int write(CommandList commandList, ByteBuffer data, int alignment) {
        commandList.uploadData(this.buffer, data, GlBufferUsage.DYNAMIC_DRAW);

        return 0;
    }

    @Override
    public GlBuffer getBuffer() {
        return this.buffer;
    }

    @Override
    public void flush(CommandList commandList) {
        commandList.uploadData(this.buffer, null, GlBufferUsage.DYNAMIC_DRAW);
    }

    @Override
    public void delete(CommandList commandList) {
        commandList.deleteBuffer(this.buffer);
    }
}
