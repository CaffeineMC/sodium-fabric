package me.jellysquid.mods.sodium.render.stream;

import me.jellysquid.mods.sodium.opengl.buffer.GlBuffer;
import me.jellysquid.mods.sodium.opengl.device.CommandList;

import java.nio.ByteBuffer;

public interface StreamingBuffer {
    default int write(CommandList commandList, ByteBuffer data) {
        return this.write(commandList, data, 1);
    }

    int write(CommandList commandList, ByteBuffer data, int alignment);

    GlBuffer getBuffer();

    void flush(CommandList commandList);

    void delete(CommandList commandList);
}
