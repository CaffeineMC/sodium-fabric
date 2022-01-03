package me.jellysquid.mods.sodium.opengl.device;

import me.jellysquid.mods.sodium.opengl.sync.GlFence;
import me.jellysquid.mods.sodium.opengl.array.VertexArray;
import me.jellysquid.mods.sodium.opengl.array.VertexArrayCommandList;
import me.jellysquid.mods.sodium.opengl.array.VertexArrayDescription;
import me.jellysquid.mods.sodium.opengl.util.EnumBitField;
import me.jellysquid.mods.sodium.opengl.buffer.GlBuffer;
import me.jellysquid.mods.sodium.opengl.buffer.GlBufferMapFlags;
import me.jellysquid.mods.sodium.opengl.buffer.GlBufferStorageFlags;
import me.jellysquid.mods.sodium.opengl.buffer.GlMappedBuffer;

import java.nio.ByteBuffer;
import java.util.function.Consumer;

public interface CommandList extends AutoCloseable {
    EnumBitField<GlBufferStorageFlags> DEFAULT_STORAGE_FLAGS = EnumBitField.empty(GlBufferStorageFlags.class);

    GlBuffer createBuffer(ByteBuffer data, EnumBitField<GlBufferStorageFlags> flags);

    default GlBuffer createBuffer(ByteBuffer data) {
        return this.createBuffer(data, DEFAULT_STORAGE_FLAGS);
    }

    GlBuffer createBuffer(long capacity, EnumBitField<GlBufferStorageFlags> flags);

    default GlBuffer createBuffer(long capacity) {
        return this.createBuffer(capacity, DEFAULT_STORAGE_FLAGS);
    }

    GlBuffer createBuffer(long capacity, Consumer<ByteBuffer> data, EnumBitField<GlBufferStorageFlags> flags);

    default GlBuffer createBuffer(long capacity, Consumer<ByteBuffer> data) {
        return this.createBuffer(capacity, data, DEFAULT_STORAGE_FLAGS);
    }

    GlMappedBuffer createMappedBuffer(long capacity, EnumBitField<GlBufferStorageFlags> storageFlags, EnumBitField<GlBufferMapFlags> mapFlags);

    <T extends Enum<T>> VertexArray<T> createVertexArray(VertexArrayDescription<T> desc);

    <T extends Enum<T>> void useVertexArray(VertexArray<T> array, Consumer<VertexArrayCommandList<T>> consumer);

    void deleteVertexArray(VertexArray<?> array);

    void copyBufferSubData(GlBuffer src, GlBuffer dst, long readOffset, long writeOffset, long bytes);

    void deleteBuffer(GlBuffer buffer);

    void flush();

    @Override
    default void close() {
        this.flush();
    }

    GlFence createFence();
}
