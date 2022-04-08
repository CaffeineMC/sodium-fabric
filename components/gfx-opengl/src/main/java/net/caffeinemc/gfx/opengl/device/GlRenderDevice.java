package net.caffeinemc.gfx.opengl.device;

import net.caffeinemc.gfx.api.buffer.*;
import net.caffeinemc.gfx.api.device.commands.PipelineGate;
import net.caffeinemc.gfx.opengl.array.GlVertexArray;
import net.caffeinemc.gfx.opengl.buffer.GlAllocatedBuffer;
import net.caffeinemc.gfx.opengl.buffer.GlBuffer;
import net.caffeinemc.gfx.opengl.buffer.GlMappedBuffer;
import net.caffeinemc.gfx.opengl.GlEnum;
import net.caffeinemc.gfx.opengl.pipeline.GlPipeline;
import net.caffeinemc.gfx.opengl.pipeline.GlPipelineManager;
import net.caffeinemc.gfx.opengl.texture.GlSampler;
import net.caffeinemc.gfx.opengl.shader.GlProgram;
import net.caffeinemc.gfx.opengl.sync.GlFence;
import net.caffeinemc.gfx.api.device.commands.RenderCommandList;
import net.caffeinemc.gfx.api.array.VertexArray;
import net.caffeinemc.gfx.api.array.VertexArrayBuffer;
import net.caffeinemc.gfx.api.array.VertexArrayDescription;
import net.caffeinemc.gfx.api.device.RenderDevice;
import net.caffeinemc.gfx.api.device.RenderDeviceProperties;
import net.caffeinemc.gfx.api.shader.Program;
import net.caffeinemc.gfx.api.shader.ShaderBindingContext;
import net.caffeinemc.gfx.api.shader.ShaderDescription;
import net.caffeinemc.gfx.api.types.ElementFormat;
import net.caffeinemc.gfx.api.pipeline.Pipeline;
import net.caffeinemc.gfx.api.types.PrimitiveType;
import net.caffeinemc.gfx.api.pipeline.PipelineDescription;
import net.caffeinemc.gfx.api.sync.Fence;
import net.caffeinemc.gfx.api.texture.Sampler;
import org.apache.commons.lang3.Validate;
import org.lwjgl.PointerBuffer;
import org.lwjgl.opengl.*;
import org.lwjgl.system.MathUtil;
import org.lwjgl.system.MemoryStack;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;

public class GlRenderDevice implements RenderDevice {
    private final GlPipelineManager pipelineManager;
    private final RenderDeviceProperties properties;

    public GlRenderDevice(GlPipelineManager pipelineManager) {
        // TODO: move this into platform code
        this.pipelineManager = pipelineManager;
        this.properties = getDeviceProperties();
    }

    private static RenderDeviceProperties getDeviceProperties() {
        var uniformBufferAlignment = GL45C.glGetInteger(GL45C.GL_UNIFORM_BUFFER_OFFSET_ALIGNMENT);

        if (!MathUtil.mathIsPoT(uniformBufferAlignment)) {
            throw new RuntimeException("GL_UNIFORM_BUFFER_OFFSET_ALIGNMENT is not a power-of-two (found value of %s)"
                    .formatted(uniformBufferAlignment));
        }

        return new RenderDeviceProperties(uniformBufferAlignment);
    }

    @Override
    public void copyBuffer(Buffer readBuffer, long readOffset, Buffer writeBuffer, long writeOffset, long bytes) {
        this.copyBuffer0((GlBuffer) readBuffer, (GlBuffer) writeBuffer, readOffset, writeOffset, bytes);
    }

    private void copyBuffer0(GlBuffer src, GlBuffer dst, long readOffset, long writeOffset, long bytes) {
        GL45C.glCopyNamedBufferSubData(src.handle(), dst.handle(), readOffset, writeOffset, bytes);
    }

    @Override
    public void deleteBuffer(Buffer buffer) {
        this.deleteBuffer0((GlBuffer) buffer);
    }

    private void deleteBuffer0(GlBuffer buffer) {
        int handle = buffer.handle();
        buffer.invalidateHandle();

        GL20C.glDeleteBuffers(handle);
    }

    @Override
    public void deleteProgram(Program<?> program) {
        this.deleteProgram0((GlProgram<?>) program);
    }

    private void deleteProgram0(GlProgram<?> program) {
        GL20C.glDeleteProgram(program.handle());
        program.invalidateHandle();
    }

    @Override
    public Fence createFence() {
        return new GlFence(GL32C.glFenceSync(GL32C.GL_SYNC_GPU_COMMANDS_COMPLETE, 0));
    }

    @Override
    public RenderDeviceProperties properties() {
        return this.properties;
    }

    @Override
    public <PROGRAM, ARRAY extends Enum<ARRAY>> Pipeline<PROGRAM, ARRAY> createPipeline(PipelineDescription state, Program<PROGRAM> program, VertexArrayDescription<ARRAY> vertexArrayDescription) {
        var vertexArray = new GlVertexArray<>(GL45C.glCreateVertexArrays(), vertexArrayDescription);

        return new GlPipeline<>(state, program, vertexArray);
    }

    @Override
    public <T> Program<T> createProgram(ShaderDescription desc, Function<ShaderBindingContext, T> interfaceFactory) {
        return new GlProgram<>(desc, interfaceFactory);
    }

    @Override
    public AllocatedBuffer allocateBuffer(long capacity, boolean client) {
        var handle = GL45C.glCreateBuffers();
        var usage = GL45C.GL_MAP_WRITE_BIT;

        if (client) {
            usage |= GL45C.GL_CLIENT_STORAGE_BIT;
        }

        GL45C.glNamedBufferStorage(handle, capacity, usage);

        var mapping = GL45C.glMapNamedBufferRange(handle, 0, capacity,
                GL45C.GL_MAP_INVALIDATE_BUFFER_BIT | GL45C.GL_MAP_WRITE_BIT | GL45C.GL_MAP_UNSYNCHRONIZED_BIT | GL45C.GL_MAP_FLUSH_EXPLICIT_BIT);

        if (mapping == null) {
            throw new RuntimeException("Failed to map buffer for writing");
        }

        return new GlAllocatedBuffer(mapping, capacity, handle);
    }

    @Override
    public Buffer createBuffer(ByteBuffer data, Set<BufferStorageFlags> flags) {
        return this.createBuffer(data.remaining(), (writer) -> {
            writer.put(data.asReadOnlyBuffer());
        }, flags);
    }

    @Override
    public Buffer createBuffer(long capacity, Set<BufferStorageFlags> flags) {
        var handle = GL45C.glCreateBuffers();
        GL45C.glNamedBufferStorage(handle, capacity, GlEnum.storageFlags(flags));

        return new GlBuffer(capacity, handle);
    }

    @Override
    public Buffer createBuffer(long capacity, Consumer<ByteBuffer> builder, Set<BufferStorageFlags> flags) {
        var handle = GL45C.glCreateBuffers();
        GL45C.glNamedBufferStorage(handle, capacity, GlEnum.storageFlags(flags) | GL45C.GL_MAP_WRITE_BIT);

        var mapping = GL45C.glMapNamedBufferRange(handle, 0, capacity,
                GL45C.GL_MAP_INVALIDATE_BUFFER_BIT | GL45C.GL_MAP_WRITE_BIT | GL45C.GL_MAP_UNSYNCHRONIZED_BIT);

        if (mapping == null) {
            throw new RuntimeException("Failed to map buffer for writing");
        }

        builder.accept(mapping);

        GL45C.glUnmapNamedBuffer(handle);

        return new GlBuffer(capacity, handle);
    }

    @Override
    public MappedBuffer createMappedBuffer(long capacity, Set<BufferStorageFlags> storageFlags, Set<BufferMapFlags> mapFlags) {
        var handle = GL45C.glCreateBuffers();
        GL45C.glNamedBufferStorage(handle, capacity, GlEnum.storageFlags(storageFlags));

        ByteBuffer data = GL45C.glMapNamedBufferRange(handle, 0, capacity, GlEnum.mapFlags(mapFlags));

        if (data == null) {
            throw new RuntimeException("Failed to map buffer");
        }

        return new GlMappedBuffer(capacity, handle, data, mapFlags);
    }

    @Override
    public <PROGRAM, ARRAY extends Enum<ARRAY>> void usePipeline(Pipeline<PROGRAM, ARRAY> pipeline, PipelineGate<PROGRAM, ARRAY> gate) {
        this.pipelineManager.bindPipeline(pipeline, (state) -> {
            gate.run(new ImmediateRenderCommandList<>((GlVertexArray<ARRAY>) pipeline.getVertexArray()), pipeline.getProgram().getInterface(), state);
        });
    }

    @Override
    public Buffer createBuffer(AllocatedBuffer buffer, int offset, int length) {
        return this.createBuffer0((GlAllocatedBuffer) buffer, offset, length);
    }

    private Buffer createBuffer0(GlAllocatedBuffer buffer, int offset, int length) {
        var handle = buffer.handle();
        buffer.invalidateHandle();

        GL45C.glFlushMappedNamedBufferRange(handle, offset, length);
        GL45C.glUnmapNamedBuffer(handle);

        return new GlBuffer(length, handle);
    }

    @Override
    public void deletePipeline(Pipeline<?, ?> pipeline) {
        this.deleteVertexArray(pipeline.getVertexArray());
    }

    @Override
    public void updateBuffer(Buffer buffer, ByteBuffer data) {
        GL45C.glNamedBufferSubData(GlBuffer.handle(buffer), 0, data);
    }

    @Override
    public Sampler createSampler() {
        return new GlSampler();
    }

    @Override
    public void deleteSampler(Sampler sampler) {
        this.deleteSampler0((GlSampler) sampler);
    }

    private void deleteSampler0(GlSampler sampler) {
        GL45C.glDeleteSamplers(sampler.handle());
        sampler.invalidateHandle();
    }

    @Override
    public void deleteVertexArray(VertexArray<?> array) {
        this.deleteVertexArray0((GlVertexArray<?>) array);
    }

    private void deleteVertexArray0(GlVertexArray<?> array) {
        GL30C.glDeleteVertexArrays(array.handle());
        array.invalidateHandle();
    }

    private static class ImmediateRenderCommandList<T extends Enum<T>> implements RenderCommandList<T> {
        private final GlVertexArray<T> array;

        private final VertexArrayBuffer[] activeVertexBuffers;
        private GlBuffer activeElementBuffer;
        private GlBuffer activeDrawIndirectBuffer;

        private boolean vertexBuffersDirty;
        private boolean elementBufferDirty;

        public ImmediateRenderCommandList(GlVertexArray<T> array) {
            this.array = array;

            this.activeVertexBuffers = new VertexArrayBuffer[array.getBufferTargets().length];
        }

        @Override
        public void bindElementBuffer(Buffer buffer) {
            this.activeElementBuffer = (GlBuffer) buffer;
            this.elementBufferDirty = true;
        }

        @Override
        public void bindVertexBuffer(T target, Buffer buffer, int offset, int stride) {
            this.activeVertexBuffers[target.ordinal()] = new VertexArrayBuffer(buffer, offset, stride);
            this.vertexBuffersDirty = true;
        }

        @Override
        public void multiDrawElementsIndirect(Buffer indirectBuffer, int indirectOffset, int indirectCount, ElementFormat elementType, PrimitiveType primitiveType) {
            this.setupIndexedRenderingState();
            this.updateDrawIndirectBuffer(indirectBuffer);
            GL43C.glMultiDrawElementsIndirect(GlEnum.from(primitiveType), GlEnum.from(elementType), indirectOffset, indirectCount, 0);
        }

        private void updateDrawIndirectBuffer(Buffer indirectBuffer) {
            if (this.activeDrawIndirectBuffer != indirectBuffer) {
                this.activeDrawIndirectBuffer = (GlBuffer) indirectBuffer;
                GL45C.glBindBuffer(GL45C.GL_DRAW_INDIRECT_BUFFER, this.activeDrawIndirectBuffer.handle());
            }
        }

        @Override
        public void multiDrawElementsBaseVertex(PointerBuffer pointer, IntBuffer count, IntBuffer baseVertex, ElementFormat indexType, PrimitiveType primitiveType) {
            this.setupIndexedRenderingState();
            GL32C.glMultiDrawElementsBaseVertex(GlEnum.from(primitiveType), count, GlEnum.from(indexType), pointer, baseVertex);
        }

        @Override
        public void drawElementsBaseVertex(PrimitiveType primitiveType, ElementFormat elementType, long elementPointer, int baseVertex, int elementCount) {
            this.setupIndexedRenderingState();
            GL32C.glDrawElementsBaseVertex(GlEnum.from(primitiveType), elementCount, GlEnum.from(elementType), elementPointer, baseVertex);
        }

        @Override
        public void drawElements(PrimitiveType primitiveType, ElementFormat elementType, long elementPointer, int elementCount) {
            this.setupIndexedRenderingState();
            GL32C.glDrawElements(GlEnum.from(primitiveType), elementCount, GlEnum.from(elementType), elementPointer);
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
            GL45C.glVertexArrayVertexBuffer(this.array.handle(), bufferIndex, GlBuffer.handle(vertexBuffer.buffer()), vertexBuffer.offset(), vertexBuffer.stride());
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

                    buffers.put(i, GlBuffer.handle(buffer));
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
