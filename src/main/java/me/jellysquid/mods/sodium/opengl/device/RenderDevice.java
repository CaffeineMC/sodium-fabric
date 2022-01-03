package me.jellysquid.mods.sodium.opengl.device;

import me.jellysquid.mods.sodium.opengl.array.VertexArray;
import me.jellysquid.mods.sodium.opengl.array.VertexArrayDescription;
import me.jellysquid.mods.sodium.opengl.buffer.Buffer;
import me.jellysquid.mods.sodium.opengl.buffer.BufferMapFlags;
import me.jellysquid.mods.sodium.opengl.buffer.BufferStorageFlags;
import me.jellysquid.mods.sodium.opengl.buffer.MappedBuffer;
import me.jellysquid.mods.sodium.opengl.shader.*;
import me.jellysquid.mods.sodium.opengl.sync.Fence;
import me.jellysquid.mods.sodium.opengl.util.EnumBitField;

import java.nio.ByteBuffer;
import java.util.function.Consumer;
import java.util.function.Function;

public interface RenderDevice {
    RenderDevice INSTANCE = new RenderDeviceImpl();

    EnumBitField<BufferStorageFlags> DEFAULT_STORAGE_FLAGS = EnumBitField.empty(BufferStorageFlags.class);

    <T> Program<T> createProgram(ShaderDescription desc, Function<ShaderBindingContext, T> interfaceFactory);

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

    <T> void useProgram(Program<T> program, ProgramGate<T> gate);

    interface ProgramGate<T> {
        void run(ProgramCommandList commandList, T programInterface);
    }

    void deleteVertexArray(VertexArray<?> array);

    void copyBuffer(Buffer src, Buffer dst, long readOffset, long writeOffset, long bytes);

    void deleteBuffer(Buffer buffer);

    void deleteProgram(Program<?> program);

    Fence createFence();
}
