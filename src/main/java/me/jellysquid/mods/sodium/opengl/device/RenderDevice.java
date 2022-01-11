package me.jellysquid.mods.sodium.opengl.device;

import me.jellysquid.mods.sodium.opengl.array.DrawCommandList;
import me.jellysquid.mods.sodium.opengl.array.VertexArray;
import me.jellysquid.mods.sodium.opengl.array.VertexArrayDescription;
import me.jellysquid.mods.sodium.opengl.buffer.Buffer;
import me.jellysquid.mods.sodium.opengl.buffer.BufferMapFlags;
import me.jellysquid.mods.sodium.opengl.buffer.BufferStorageFlags;
import me.jellysquid.mods.sodium.opengl.buffer.MappedBuffer;
import me.jellysquid.mods.sodium.opengl.pipeline.Pipeline;
import me.jellysquid.mods.sodium.opengl.pipeline.PipelineState;
import me.jellysquid.mods.sodium.opengl.sampler.Sampler;
import me.jellysquid.mods.sodium.opengl.shader.Program;
import me.jellysquid.mods.sodium.opengl.shader.ShaderBindingContext;
import me.jellysquid.mods.sodium.opengl.shader.ShaderDescription;
import me.jellysquid.mods.sodium.opengl.sync.Fence;
import me.jellysquid.mods.sodium.opengl.types.RenderState;
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

    <PROGRAM, ARRAY extends Enum<ARRAY>> Pipeline<PROGRAM, ARRAY> createPipeline(RenderState state, Program<PROGRAM> program, VertexArrayDescription<ARRAY> vertexArray);

    <PROGRAM, ARRAY extends Enum<ARRAY>> void usePipeline(Pipeline<PROGRAM, ARRAY> pipeline, PipelineGate<PROGRAM, ARRAY> gate);

    void deletePipeline(Pipeline<?, ?> pipeline);

    void uploadData(Buffer buffer, ByteBuffer data);

    interface PipelineGate<SHADER, VERTEX extends Enum<VERTEX>> {
        void run(DrawCommandList<VERTEX> commandList, SHADER programInterface, PipelineState pipelineState);
    }

    Sampler createSampler();

    void deleteSampler(Sampler sampler);

    void deleteVertexArray(VertexArray<?> array);

    void copyBuffer(long bytes, Buffer src, long readOffset, Buffer dst, long writeOffset);

    void deleteBuffer(Buffer buffer);

    void deleteProgram(Program<?> program);

    Fence createFence();

    RenderDeviceProperties properties();
}
