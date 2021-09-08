package me.jellysquid.mods.thingl.device;

import me.jellysquid.mods.thingl.buffer.*;
import me.jellysquid.mods.thingl.util.EnumBitField;

import java.nio.ByteBuffer;

public interface ResourceAccess {
    void allocateStorage(MutableBuffer buffer, long bufferSize, BufferUsage usage);

    void uploadData(MutableBuffer glBuffer, ByteBuffer byteBuffer, BufferUsage usage);

    void copyBufferSubData(Buffer src, Buffer dst, long readOffset, long writeOffset, long bytes);

    BufferMapping mapBuffer(Buffer buffer, long offset, long length, EnumBitField<BufferMapFlags> flags);

    void unmap(BufferMapping map);

    void flushMappedRange(BufferMapping map, int offset, int length);
}
