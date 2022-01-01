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
    public BufferHandle write(CommandList commandList, ByteBuffer data, int alignment) {
        commandList.uploadData(this.buffer, data, GlBufferUsage.DYNAMIC_DRAW);

        return new Handle(0, alignment, data.remaining());
    }

    @Override
    public GlBuffer getBuffer() {
        return this.buffer;
    }

    @Override
    public void delete(CommandList commandList) {
        commandList.deleteBuffer(this.buffer);
    }

    private static class Handle extends AbstractBufferHandle {
        Handle(int offset, int stride, int length) {
            super(offset, stride, length);
        }

        @Override
        public void finish(Supplier<GlFence> fence) {

        }
    }
}
