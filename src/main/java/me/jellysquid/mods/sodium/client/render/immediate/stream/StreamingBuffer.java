package me.jellysquid.mods.sodium.client.render.immediate.stream;

import me.jellysquid.mods.sodium.client.gl.buffer.GlBuffer;
import me.jellysquid.mods.sodium.client.gl.device.CommandList;

import java.nio.ByteBuffer;

public interface StreamingBuffer {
    int write(CommandList commandList, ByteBuffer data, int alignment);

    GlBuffer getBuffer();

    void flush(CommandList commandList);

    void delete(CommandList commandList);
}
