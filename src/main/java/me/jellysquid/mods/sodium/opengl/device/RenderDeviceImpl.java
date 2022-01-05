package me.jellysquid.mods.sodium.opengl.device;

import me.jellysquid.mods.sodium.opengl.array.*;
import me.jellysquid.mods.sodium.opengl.buffer.*;
import me.jellysquid.mods.sodium.opengl.pipeline.Blaze3DPipelineState;
import me.jellysquid.mods.sodium.opengl.pipeline.PipelineCommandList;
import me.jellysquid.mods.sodium.opengl.pipeline.PipelineState;
import me.jellysquid.mods.sodium.opengl.sampler.Sampler;
import me.jellysquid.mods.sodium.opengl.sampler.SamplerImpl;
import me.jellysquid.mods.sodium.opengl.shader.*;
import me.jellysquid.mods.sodium.opengl.sync.Fence;
import me.jellysquid.mods.sodium.opengl.sync.FenceImpl;
import me.jellysquid.mods.sodium.opengl.types.IntType;
import me.jellysquid.mods.sodium.opengl.types.PrimitiveType;
import me.jellysquid.mods.sodium.opengl.types.RenderPipeline;
import me.jellysquid.mods.sodium.opengl.util.EnumBitField;
import net.minecraft.client.render.BufferRenderer;
import org.apache.commons.lang3.Validate;
import org.lwjgl.PointerBuffer;
import org.lwjgl.opengl.GL20C;
import org.lwjgl.opengl.GL30C;
import org.lwjgl.opengl.GL32C;
import org.lwjgl.opengl.GL45C;
import org.lwjgl.system.MemoryStack;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.function.Consumer;
import java.util.function.Function;

public class RenderDeviceImpl implements RenderDevice {
    private final PipelineState pipelineState;

    public RenderDeviceImpl() {
        // TODO: move this into platform code
        this.pipelineState = new Blaze3DPipelineState();
    }

    @Override
    public void copyBuffer(Buffer src, Buffer dst, long readOffset, long writeOffset, long bytes) {
        this.copyBuffer0((BufferImpl) src, (BufferImpl) dst, readOffset, writeOffset, bytes);
    }

    private void copyBuffer0(BufferImpl src, BufferImpl dst, long readOffset, long writeOffset, long bytes) {
        GL45C.glCopyNamedBufferSubData(src.handle(), dst.handle(), readOffset, writeOffset, bytes);
    }

    @Override
    public void deleteBuffer(Buffer buffer) {
        this.deleteBuffer0((BufferImpl) buffer);
    }

    private void deleteBuffer0(BufferImpl buffer) {
        int handle = buffer.handle();
        buffer.invalidateHandle();

        GL20C.glDeleteBuffers(handle);
    }

    @Override
    public void deleteProgram(Program<?> program) {
        this.deleteProgram0((ProgramImpl<?>) program);
    }

    private void deleteProgram0(ProgramImpl<?> program) {
        GL20C.glDeleteProgram(program.handle());
        program.invalidateHandle();
    }

    @Override
    public Fence createFence() {
        return new FenceImpl(GL32C.glFenceSync(GL32C.GL_SYNC_GPU_COMMANDS_COMPLETE, 0));
    }

    @Override
    public <T> Program<T> createProgram(ShaderDescription desc, Function<ShaderBindingContext, T> interfaceFactory) {
        return new ProgramImpl<>(desc, interfaceFactory);
    }

    @Override
    public Buffer createBuffer(ByteBuffer data, EnumBitField<BufferStorageFlags> flags) {
        return this.createBuffer(data.remaining(), (writer) -> {
            writer.put(data.asReadOnlyBuffer());
        }, flags);
    }

    @Override
    public Buffer createBuffer(long capacity, EnumBitField<BufferStorageFlags> flags) {
        var handle = GL45C.glCreateBuffers();
        GL45C.glNamedBufferStorage(handle, capacity, flags.getBitField());

        return new BufferImpl(capacity, handle);
    }

    @Override
    public Buffer createBuffer(long capacity, Consumer<ByteBuffer> builder, EnumBitField<BufferStorageFlags> flags) {
        var handle = GL45C.glCreateBuffers();
        GL45C.glNamedBufferStorage(handle, capacity, flags.getBitField() | GL45C.GL_MAP_WRITE_BIT);

        var mapping = GL45C.glMapNamedBufferRange(handle, 0, capacity,
                GL45C.GL_MAP_INVALIDATE_BUFFER_BIT | GL45C.GL_MAP_WRITE_BIT | GL45C.GL_MAP_UNSYNCHRONIZED_BIT);

        if (mapping == null) {
            throw new RuntimeException("Failed to map buffer for writing");
        }

        builder.accept(mapping);

        GL45C.glUnmapNamedBuffer(handle);

        return new BufferImpl(capacity, handle);
    }

    @Override
    public MappedBuffer createMappedBuffer(long capacity, EnumBitField<BufferStorageFlags> storageFlags, EnumBitField<BufferMapFlags> mapFlags) {
        var handle = GL45C.glCreateBuffers();
        GL45C.glNamedBufferStorage(handle, capacity, storageFlags.getBitField());

        ByteBuffer data = GL45C.glMapNamedBufferRange(handle, 0, capacity, mapFlags.getBitField());

        if (data == null) {
            throw new RuntimeException("Failed to map buffer");
        }

        return new MappedBufferImpl(capacity, handle, data);
    }

    @Override
    public <T extends Enum<T>> VertexArray<T> createVertexArray(VertexArrayDescription<T> desc) {
        return new VertexArrayImpl<>(GL45C.glCreateVertexArrays(), desc);
    }

    @Override
    public <T> void usePipeline(RenderPipeline pipeline, PipelineGate gate) {
        BufferRenderer.unbindAll(); // TODO: move this into platform code

        try {
            if (pipeline != null) pipeline.enable(); // TODO: require a valid pipeline object,

            gate.run(new ImmediatePipelineCommandList(), this.pipelineState);
        } finally {
            this.pipelineState.restoreState();
        }
    }

    @Override
    public Sampler createSampler() {
        return new SamplerImpl();
    }

    @Override
    public void deleteSampler(Sampler sampler) {
        this.deleteSampler0((SamplerImpl) sampler);
    }

    private void deleteSampler0(SamplerImpl sampler) {
        GL45C.glDeleteSamplers(sampler.handle());
        sampler.invalidateHandle();
    }

    @Override
    public void deleteVertexArray(VertexArray<?> array) {
        this.deleteVertexArray0((VertexArrayImpl<?>) array);
    }

    private void deleteVertexArray0(VertexArrayImpl<?> array) {
        GL30C.glDeleteVertexArrays(array.handle());
        array.invalidateHandle();
    }

    private static class ImmediateVertexArrayCommandList<T extends Enum<T>> implements VertexArrayCommandList<T> {
        private final VertexArrayImpl<T> array;

        private VertexArrayResourceSet<T> activeVertexBuffers;
        private Buffer activeElementBuffer;

        public ImmediateVertexArrayCommandList(VertexArrayImpl<T> array) {
            this.array = array;
        }

        @Override
        public void bindVertexBuffers(VertexArrayResourceSet<T> bindings) {
            var slots = bindings.slots;

            try (MemoryStack stack = MemoryStack.stackPush()) {
                var buffers = stack.callocInt(slots.length);
                var offsets = stack.callocPointer(slots.length);
                var strides = stack.callocInt(slots.length);

                for (int i = 0; i < slots.length; i++) {
                    var slot = slots[i];

                    var buffer = slot.buffer();
                    var stride = slot.stride();

                    buffers.put(i, buffer.handle());
                    offsets.put(i, 0 /* TODO: allow specifying an offset */);
                    strides.put(i, stride);
                }

                GL45C.glVertexArrayVertexBuffers(this.array.handle(), 0, buffers, offsets, strides);
            }

            this.activeVertexBuffers = bindings;
        }

        @Override
        public void bindElementBuffer(Buffer buffer) {
            GL45C.glVertexArrayElementBuffer(this.array.handle(), buffer.handle());
            this.activeElementBuffer = buffer;
        }

        @Override
        public void multiDrawElementsBaseVertex(PointerBuffer pointer, IntBuffer count, IntBuffer baseVertex, IntType indexType, PrimitiveType primitiveType) {
            this.checkIndexedResources();
            GL32C.glMultiDrawElementsBaseVertex(primitiveType.getId(), count, indexType.getFormatId(), pointer, baseVertex);
        }

        @Override
        public void drawElementsBaseVertex(PrimitiveType primitiveType, IntType elementType, long elementPointer, int baseVertex, int elementCount) {
            this.checkIndexedResources();
            GL32C.glDrawElementsBaseVertex(primitiveType.getId(), elementCount, elementType.getFormatId(), elementPointer, baseVertex);
        }

        private void checkIndexedResources() {
            Validate.notNull(this.activeVertexBuffers, "Vertex buffers not bound");
            Validate.notNull(this.activeElementBuffer, "Element buffer not bound");
        }
    }

    private static class ImmediateProgramCommandList implements ProgramCommandList {
        @Override
        public <A extends Enum<A>> void useVertexArray(VertexArray<A> array, Consumer<VertexArrayCommandList<A>> consumer) {
            this.useVertexArray0((VertexArrayImpl<A>) array, consumer);
        }

        private <A extends Enum<A>> void useVertexArray0(VertexArrayImpl<A> array, Consumer<VertexArrayCommandList<A>> consumer) {
            GL30C.glBindVertexArray(array.handle());
            consumer.accept(new ImmediateVertexArrayCommandList<>(array));
        }
    }

    private static class ImmediatePipelineCommandList implements PipelineCommandList {
        @Override
        public <T> void useProgram(Program<T> program, ProgramGate<T> gate) {
            GL30C.glUseProgram(program.handle());
            gate.run(new ImmediateProgramCommandList(), program.getInterface());
        }
    }
}
