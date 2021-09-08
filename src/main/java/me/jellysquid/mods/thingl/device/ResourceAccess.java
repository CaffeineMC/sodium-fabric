package me.jellysquid.mods.thingl.device;

import me.jellysquid.mods.thingl.array.GlVertexArray;
import me.jellysquid.mods.thingl.buffer.*;
import me.jellysquid.mods.thingl.util.EnumBitField;

import java.nio.ByteBuffer;

public interface ResourceAccess {
    void allocateStorage(GlMutableBuffer buffer, long bufferSize, GlBufferUsage usage);

    void uploadData(GlMutableBuffer glBuffer, ByteBuffer byteBuffer, GlBufferUsage usage);

    void copyBufferSubData(GlBuffer src, GlBuffer dst, long readOffset, long writeOffset, long bytes);

    GlBufferMapping mapBuffer(GlBuffer buffer, long offset, long length, EnumBitField<GlBufferMapFlags> flags);

    void unmap(GlBufferMapping map);

    void flushMappedRange(GlBufferMapping map, int offset, int length);
}
