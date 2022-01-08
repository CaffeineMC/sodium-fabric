package me.jellysquid.mods.sodium.opengl.device;

import me.jellysquid.mods.sodium.opengl.array.VertexArray;
import me.jellysquid.mods.sodium.opengl.array.VertexArrayDescription;
import me.jellysquid.mods.sodium.opengl.buffer.*;
import me.jellysquid.mods.sodium.opengl.pipeline.PipelineCommandList;
import me.jellysquid.mods.sodium.opengl.pipeline.PipelineState;
import me.jellysquid.mods.sodium.opengl.sampler.Sampler;
import me.jellysquid.mods.sodium.opengl.shader.Program;
import me.jellysquid.mods.sodium.opengl.shader.ProgramCommandList;
import me.jellysquid.mods.sodium.opengl.shader.ShaderBindingContext;
import me.jellysquid.mods.sodium.opengl.shader.ShaderDescription;
import me.jellysquid.mods.sodium.opengl.sync.Fence;
import me.jellysquid.mods.sodium.opengl.types.RenderPipeline;
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

    FlushableMappedBuffer createFlushableMappedBuffer(long capacity, EnumBitField<BufferStorageFlags> storageFlags, EnumBitField<BufferMapFlags> mapFlags);

    <T extends Enum<T>> VertexArray<T> createVertexArray(VertexArrayDescription<T> desc);

    <T> void usePipeline(RenderPipeline pipeline, PipelineGate gate);

    interface PipelineGate {
        void run(PipelineCommandList commandList, PipelineState pipelineState);
    }

    Sampler createSampler();

    void deleteSampler(Sampler sampler);

    void deleteVertexArray(VertexArray<?> array);

    void copyBuffer(Buffer src, Buffer dst, long readOffset, long writeOffset, long bytes);

    void deleteBuffer(Buffer buffer);

    void deleteProgram(Program<?> program);

    Fence createFence();
}
