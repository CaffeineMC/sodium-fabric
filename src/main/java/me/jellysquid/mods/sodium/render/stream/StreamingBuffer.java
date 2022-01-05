package me.jellysquid.mods.sodium.render.stream;

import me.jellysquid.mods.sodium.opengl.buffer.Buffer;

import java.nio.ByteBuffer;

public interface StreamingBuffer {
    default int write(ByteBuffer data) {
        return this.write(data, 1);
    }

    int write(ByteBuffer data, int alignment);

    Buffer getBuffer();

    void flush();

    void delete();

    String getDebugString();
}
