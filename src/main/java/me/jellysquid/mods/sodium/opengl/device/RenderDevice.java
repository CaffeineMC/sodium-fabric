package me.jellysquid.mods.sodium.opengl.device;

import me.jellysquid.mods.sodium.opengl.array.VertexArray;
import me.jellysquid.mods.sodium.opengl.array.VertexArrayCommandList;
import me.jellysquid.mods.sodium.opengl.array.VertexArrayDescription;
import me.jellysquid.mods.sodium.opengl.buffer.*;
import me.jellysquid.mods.sodium.opengl.sync.Fence;
import me.jellysquid.mods.sodium.opengl.util.EnumBitField;

import java.nio.ByteBuffer;
import java.util.function.Consumer;

public interface RenderDevice {
    RenderDevice INSTANCE = new RenderDeviceImpl();

    EnumBitField<BufferStorageFlags> DEFAULT_STORAGE_FLAGS = EnumBitField.empty(BufferStorageFlags.class);

    Buffer createBuffer(ByteBuffer data, EnumBitField<BufferStorageFlags> flags);

    default Buffer createBuffer(ByteBuffer data) {
        return this.createBuffer(data, DEFAULT_STORAGE_FLAGS);
    }

    Buffer createBuffer(long capacity, EnumBitField<BufferStorageFlags> flags);

    default Buffer createBuffer(long capacity) {
        return this.createBuffer(capacity, DEFAULT_STORAGE_FLAGS);
    }

    Buffer createBuffer(long capacity, Consumer<ByteBuffer> data, EnumBitField<BufferStorageFlags> flags);

    default Buffer createBuffer(long capacity, Consumer<ByteBuffer> data) {
        return this.createBuffer(capacity, data, DEFAULT_STORAGE_FLAGS);
    }

    MappedBuffer createMappedBuffer(long capacity, EnumBitField<BufferStorageFlags> storageFlags, EnumBitField<BufferMapFlags> mapFlags);

    <T extends Enum<T>> VertexArray<T> createVertexArray(VertexArrayDescription<T> desc);

    <T extends Enum<T>> void useVertexArray(VertexArray<T> array, Consumer<VertexArrayCommandList<T>> consumer);

    void deleteVertexArray(VertexArray<?> array);

    void copyBuffer(Buffer src, Buffer dst, long readOffset, long writeOffset, long bytes);

    void deleteBuffer(Buffer buffer);

    Fence createFence();
}
