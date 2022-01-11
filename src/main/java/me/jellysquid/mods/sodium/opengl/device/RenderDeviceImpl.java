package me.jellysquid.mods.sodium.opengl.device;

import me.jellysquid.mods.sodium.opengl.array.*;
import me.jellysquid.mods.sodium.opengl.buffer.*;
import me.jellysquid.mods.sodium.opengl.pipeline.Blaze3DPipelineManager;
import me.jellysquid.mods.sodium.opengl.pipeline.Pipeline;
import me.jellysquid.mods.sodium.opengl.pipeline.PipelineImpl;
import me.jellysquid.mods.sodium.opengl.pipeline.PipelineManager;
import me.jellysquid.mods.sodium.opengl.sampler.Sampler;
import me.jellysquid.mods.sodium.opengl.sampler.SamplerImpl;
import me.jellysquid.mods.sodium.opengl.shader.Program;
import me.jellysquid.mods.sodium.opengl.shader.ProgramImpl;
import me.jellysquid.mods.sodium.opengl.shader.ShaderBindingContext;
import me.jellysquid.mods.sodium.opengl.shader.ShaderDescription;
import me.jellysquid.mods.sodium.opengl.sync.Fence;
import me.jellysquid.mods.sodium.opengl.sync.FenceImpl;
import me.jellysquid.mods.sodium.opengl.types.IntType;
import me.jellysquid.mods.sodium.opengl.types.PrimitiveType;
import me.jellysquid.mods.sodium.opengl.types.RenderState;
import me.jellysquid.mods.sodium.opengl.util.EnumBitField;
import org.apache.commons.lang3.Validate;
import org.lwjgl.PointerBuffer;
import org.lwjgl.opengl.*;
import org.lwjgl.system.MemoryStack;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.function.Consumer;
import java.util.function.Function;

public class RenderDeviceImpl implements RenderDevice {
    private final PipelineManager pipelineManager;
    private final RenderDeviceProperties properties;

    public RenderDeviceImpl() {
        // TODO: move this into platform code
        this.pipelineManager = new Blaze3DPipelineManager();
        this.properties = new RenderDeviceProperties();
    }

    @Override
    public void copyBuffer(long bytes, Buffer src, long readOffset, Buffer dst, long writeOffset) {
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
    public RenderDeviceProperties properties() {
        return this.properties;
    }

    @Override
    public <PROGRAM, ARRAY extends Enum<ARRAY>> Pipeline<PROGRAM, ARRAY> createPipeline(RenderState state, Program<PROGRAM> program, VertexArrayDescription<ARRAY> vertexArrayDescription) {
        var vertexArray = new VertexArrayImpl<>(GL45C.glCreateVertexArrays(), vertexArrayDescription);

        return new PipelineImpl<>(state, program, vertexArray);
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

        return new MappedBufferImpl(capacity, handle, data, mapFlags);
    }

    @Override
    public <PROGRAM, ARRAY extends Enum<ARRAY>> void usePipeline(Pipeline<PROGRAM, ARRAY> pipeline, PipelineGate<PROGRAM, ARRAY> gate) {
        this.pipelineManager.bindPipeline(pipeline, (state) -> {
            gate.run(new ImmediateDrawCommandList<>(pipeline.getVertexArray()), pipeline.getProgram().getInterface(), state);
        });
    }

    @Override
    public void deletePipeline(Pipeline<?, ?> pipeline) {
        this.deleteVertexArray(pipeline.getVertexArray());
    }

    @Override
    public void uploadData(Buffer buffer, ByteBuffer data) {
        GL45C.glNamedBufferSubData(buffer.handle(), 0, data);
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

    private static class ImmediateDrawCommandList<T extends Enum<T>> implements DrawCommandList<T> {
        private final VertexArray<T> array;

        private final VertexArrayBuffer[] activeVertexBuffers;
        private Buffer activeElementBuffer;
        private Buffer activeDrawIndirectBuffer;

        private boolean vertexBuffersDirty;
        private boolean elementBufferDirty;

        public ImmediateDrawCommandList(VertexArray<T> array) {
            this.array = array;

            this.activeVertexBuffers = new VertexArrayBuffer[array.getBufferTargets().length];
        }

        @Override
        public void bindElementBuffer(Buffer buffer) {
            this.activeElementBuffer = buffer;
            this.elementBufferDirty = true;
        }

        @Override
        public void bindVertexBuffer(T target, Buffer buffer, int offset, int stride) {
            this.activeVertexBuffers[target.ordinal()] = new VertexArrayBuffer(buffer, offset, stride);
            this.vertexBuffersDirty = true;
        }

        @Override
        public void multiDrawElementsIndirect(Buffer indirectBuffer, int indirectOffset, int indirectCount, IntType elementType, PrimitiveType primitiveType) {
            this.setupIndexedRenderingState();
            this.updateDrawIndirectBuffer(indirectBuffer);
            GL43C.glMultiDrawElementsIndirect(primitiveType.getId(), elementType.getFormatId(), indirectOffset, indirectCount, 0);
        }

        private void updateDrawIndirectBuffer(Buffer indirectBuffer) {
            if (this.activeDrawIndirectBuffer != indirectBuffer) {
                GL45C.glBindBuffer(GL45C.GL_DRAW_INDIRECT_BUFFER, indirectBuffer.handle());
                this.activeDrawIndirectBuffer = indirectBuffer;
            }
        }

        @Override
        public void multiDrawElementsBaseVertex(PointerBuffer pointer, IntBuffer count, IntBuffer baseVertex, IntType indexType, PrimitiveType primitiveType) {
            this.setupIndexedRenderingState();
            GL32C.glMultiDrawElementsBaseVertex(primitiveType.getId(), count, indexType.getFormatId(), pointer, baseVertex);
        }

        @Override
        public void drawElementsBaseVertex(PrimitiveType primitiveType, IntType elementType, long elementPointer, int baseVertex, int elementCount) {
            this.setupIndexedRenderingState();
            GL32C.glDrawElementsBaseVertex(primitiveType.getId(), elementCount, elementType.getFormatId(), elementPointer, baseVertex);
        }

        @Override
        public void drawElements(PrimitiveType primitiveType, IntType elementType, long elementPointer, int elementCount) {
            this.setupIndexedRenderingState();
            GL32C.glDrawElements(primitiveType.getId(), elementCount, elementType.getFormatId(), elementPointer);
        }

        private void setupIndexedRenderingState() {
            this.validateElementBuffer();
            this.validateVertexBuffers();
            this.bindBuffers();
        }

        private void validateElementBuffer() {
            Validate.notNull(this.activeElementBuffer, "Element buffer not bound");
        }

        private void validateVertexBuffers() {
            for (int i = 0; i < this.activeVertexBuffers.length; i++) {
                if (this.activeVertexBuffers[i] == null) {
                    throw new IllegalStateException("No vertex buffer bound to target: " + this.array.getBufferTargets()[i]);
                }
            }
        }

        private void bindBuffers() {
            if (this.vertexBuffersDirty) {
                this.bindVertexBuffers();
            }

            if (this.elementBufferDirty) {
                this.bindElementBuffer();
            }
        }

        private void bindVertexBuffers() {
            if (this.activeVertexBuffers.length <= 1) {
                this.bindVertexBuffersOneshot();
            } else {
                this.bindVertexBuffersMulti();
            }
        }

        private void bindVertexBuffersOneshot() {
            for (int bufferIndex = 0; bufferIndex < this.activeVertexBuffers.length; bufferIndex++) {
                this.bindVertexBuffer(bufferIndex, this.activeVertexBuffers[bufferIndex]);
            }
        }

        private void bindVertexBuffer(int bufferIndex, VertexArrayBuffer vertexBuffer) {
            GL45C.glVertexArrayVertexBuffer(this.array.handle(), bufferIndex, vertexBuffer.buffer().handle(), vertexBuffer.offset(), vertexBuffer.stride());
        }

        private void bindVertexBuffersMulti() {
            var count = this.activeVertexBuffers.length;

            try (MemoryStack stack = MemoryStack.stackPush()) {
                var buffers = stack.callocInt(count);
                var offsets = stack.callocPointer(count);
                var strides = stack.callocInt(count);

                for (int i = 0; i < count; i++) {
                    var binding = this.activeVertexBuffers[i];

                    var buffer = binding.buffer();
                    var offset = binding.offset();
                    var stride = binding.stride();

                    buffers.put(i, buffer.handle());
                    offsets.put(i, offset);
                    strides.put(i, stride);
                }

                GL45C.glVertexArrayVertexBuffers(this.array.handle(), 0, buffers, offsets, strides);
            }

            this.vertexBuffersDirty = false;
        }

        private void bindElementBuffer() {
            GL45C.glVertexArrayElementBuffer(this.array.handle(), this.activeElementBuffer != null ? this.activeElementBuffer.handle() : 0);
            this.elementBufferDirty = false;
        }
    }
}
