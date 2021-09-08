package me.jellysquid.mods.thingl.device;

import me.jellysquid.mods.thingl.array.VertexArray;
import me.jellysquid.mods.thingl.array.VertexArrayImpl;
import me.jellysquid.mods.thingl.buffer.*;
import me.jellysquid.mods.thingl.functions.DeviceFunctions;
import me.jellysquid.mods.thingl.functions.DirectStateAccessFunctions;
import me.jellysquid.mods.thingl.lists.PipelineCommandList;
import me.jellysquid.mods.thingl.lists.ShaderCommandList;
import me.jellysquid.mods.thingl.lists.TessellationCommandList;
import me.jellysquid.mods.thingl.pipeline.RenderPipeline;
import me.jellysquid.mods.thingl.shader.*;
import me.jellysquid.mods.thingl.state.StateTracker;
import me.jellysquid.mods.thingl.sync.Fence;
import me.jellysquid.mods.thingl.sync.FenceImpl;
import me.jellysquid.mods.thingl.tessellation.*;
import me.jellysquid.mods.thingl.tessellation.binding.ElementBufferBinding;
import me.jellysquid.mods.thingl.tessellation.binding.VertexBufferBinding;
import me.jellysquid.mods.thingl.texture.Sampler;
import me.jellysquid.mods.thingl.texture.SamplerImpl;
import me.jellysquid.mods.thingl.texture.Texture;
import me.jellysquid.mods.thingl.texture.TextureImpl;
import me.jellysquid.mods.thingl.util.EnumBitField;
import org.lwjgl.PointerBuffer;
import org.lwjgl.opengl.*;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.function.Consumer;
import java.util.function.Function;

public class RenderDeviceImpl implements RenderDevice {
    private final DeviceFunctions functions = new DeviceFunctions(this);
    private final StateTracker stateTracker;

    RenderDeviceImpl(StateTracker tracker) {
        this.stateTracker = tracker;
    }

    public GLCapabilities getCapabilities() {
        return GL.getCapabilities();
    }

    @Override
    public DeviceFunctions getDeviceFunctions() {
        return this.functions;
    }

    public StateTracker getStateTracker() {
        return this.stateTracker;
    }

    @Override
    public void usePipeline(RenderPipeline pipeline, Consumer<PipelineCommandList> consumer) {
        consumer.accept(new ImmediatePipelineCommandList(pipeline));
    }

    @Override
    public Shader createShader(ShaderType type, String source) {
        return new ShaderImpl(this, type, source);
    }

    @Override
    public <T> Program<T> createProgram(Shader[] shaders, Function<ShaderBindingContext, T> interfaceFactory) {
        return new ProgramImpl<>(this, shaders, interfaceFactory);
    }

    @Override
    public Tessellation createTessellation(PrimitiveType primitiveType, VertexBufferBinding[] vertexBindings, ElementBufferBinding elementBinding) {
        if (this.functions.getDirectStateAccessFunctions() == DirectStateAccessFunctions.NONE) {
            return new TessellationImpl.FallbackTessellationImpl(this, primitiveType, vertexBindings, elementBinding);
        }

        return new TessellationImpl.BindlessTessellationImpl(this, primitiveType, vertexBindings, elementBinding);
    }

    @Override
    public MutableBuffer createMutableBuffer() {
        return new MutableBufferImpl(this,
                this.getDeviceFunctions().getDirectStateAccessFunctions() != DirectStateAccessFunctions.NONE);
    }

    @Override
    public ImmutableBuffer createImmutableBuffer(long bufferSize, EnumBitField<BufferStorageFlags> flags) {
        ImmutableBufferImpl buffer = new ImmutableBufferImpl(this, flags,
                this.getDeviceFunctions().getDirectStateAccessFunctions() != DirectStateAccessFunctions.NONE);
        buffer.createBufferStorage(bufferSize);

        return buffer;
    }

    @Override
    public Sampler createSampler() {
        return new SamplerImpl(this);
    }

    @Override
    public void uploadData(MutableBuffer buffer, ByteBuffer data, BufferUsage usage) {
        this.uploadData0((MutableBufferImpl) buffer, data, usage);
    }

    private void uploadData0(MutableBufferImpl buffer, ByteBuffer data, BufferUsage usage) {
        buffer.upload(data, usage);
    }

    @Override
    public void copyBufferSubData(Buffer src, Buffer dst, long readOffset, long writeOffset, long bytes) {
        this.copyBufferSubData0((BufferImpl) src, (BufferImpl) dst, readOffset, writeOffset, bytes);
    }

    private void copyBufferSubData0(BufferImpl src, BufferImpl dst, long readOffset, long writeOffset, long bytes) {
        if (this.functions.getDirectStateAccessFunctions() == DirectStateAccessFunctions.NONE) {
            src.bind(BufferTarget.COPY_READ_BUFFER);
            dst.bind(BufferTarget.COPY_WRITE_BUFFER);

            GL31C.glCopyBufferSubData(GL31C.GL_COPY_READ_BUFFER, GL31C.GL_COPY_WRITE_BUFFER, readOffset, writeOffset, bytes);
        } else {
            this.functions.getDirectStateAccessFunctions()
                    .copyNamedBufferSubData(src.handle(), dst.handle(), readOffset, writeOffset, bytes);
        }
    }

    @Override
    public void deleteTessellation(Tessellation tessellation) {
        this.deleteTessellation0((TessellationImpl) tessellation);
    }

    private void deleteTessellation0(TessellationImpl tessellation) {
        tessellation.delete();
    }

    @Override
    public BufferMapping mapBuffer(Buffer buffer, long offset, long length, EnumBitField<BufferMapFlags> flags) {
        return this.mapBuffer0((BufferImpl) buffer, offset, length, flags);
    }

    private BufferMapping mapBuffer0(BufferImpl buffer, long offset, long length, EnumBitField<BufferMapFlags> flags) {
        if (buffer.getActiveMapping() != null) {
            throw new IllegalStateException("Buffer is already mapped");
        }

        if (flags.contains(BufferMapFlags.PERSISTENT) && !(buffer instanceof ImmutableBufferImpl)) {
            throw new IllegalStateException("Tried to map mutable buffer as persistent");
        }

        // TODO: speed this up?
        if (buffer instanceof ImmutableBufferImpl) {
            EnumBitField<BufferStorageFlags> bufferFlags = ((ImmutableBufferImpl) buffer).getFlags();

            if (flags.contains(BufferMapFlags.PERSISTENT) && !bufferFlags.contains(BufferStorageFlags.PERSISTENT)) {
                throw new IllegalArgumentException("Tried to map non-persistent buffer as persistent");
            }

            if (flags.contains(BufferMapFlags.WRITE) && !bufferFlags.contains(BufferStorageFlags.MAP_WRITE)) {
                throw new IllegalStateException("Tried to map non-writable buffer as writable");
            }

            if (flags.contains(BufferMapFlags.READ) && !bufferFlags.contains(BufferStorageFlags.MAP_READ)) {
                throw new IllegalStateException("Tried to map non-readable buffer as readable");
            }
        }

        return buffer.createMapping(offset, length, flags);
    }

    @Override
    public void unmap(BufferMapping map) {
        this.unmap0((BufferMappingImpl) map);
    }

    private void unmap0(BufferMappingImpl map) {
        map.checkDisposed();

        BufferImpl buffer = map.getBufferObject();
        buffer.unmap();

        map.dispose();
    }

    @Override
    public void flushMappedRange(BufferMapping map, int offset, int length) {
        this.flushMappedRange0((BufferMappingImpl) map, offset, length);
    }

    private void flushMappedRange0(BufferMappingImpl map, int offset, int length) {
        map.checkDisposed();

        BufferImpl buffer = map.getBufferObject();

        buffer.bind(BufferTarget.COPY_READ_BUFFER);
        GL32C.glFlushMappedBufferRange(BufferTarget.COPY_READ_BUFFER.getTargetParameter(), offset, length);
    }

    @Override
    public Fence createFence() {
        return new FenceImpl();
    }

    @Override
    public Texture createTexture() {
        return new TextureImpl(this);
    }

    @Override
    public void allocateStorage(MutableBuffer buffer, long bufferSize, BufferUsage usage) {
        this.allocateStorage0((MutableBufferImpl) buffer, bufferSize, usage);
    }

    private void allocateStorage0(MutableBufferImpl buffer, long bufferSize, BufferUsage usage) {
        buffer.bind(BufferTarget.ARRAY_BUFFER);

        GL20C.glBufferData(BufferTarget.ARRAY_BUFFER.getTargetParameter(), bufferSize, usage.getId());
        buffer.setSize(bufferSize);
    }

    @Override
    public void deleteBuffer(Buffer buffer) {
        this.deleteBuffer0((BufferImpl) buffer);
    }

    private void deleteBuffer0(BufferImpl buffer) {
        if (buffer.getActiveMapping() != null) {
            this.unmap(buffer.getActiveMapping());
        }

        this.stateTracker.notifyBufferDeleted(buffer);

        int handle = buffer.handle();
        buffer.invalidateHandle();

        GL20C.glDeleteBuffers(handle);
    }

    @Override
    public void deleteVertexArray(VertexArray vertexArray) {
        this.deleteVertexArray0((VertexArrayImpl) vertexArray);
    }

    @Override
    public void deleteProgram(Program<?> program) {
        this.deleteProgram0((ProgramImpl<?>) program);
    }

    @Override
    public void deleteShader(Shader shader) {
        this.deleteShader0((ShaderImpl) shader);
    }

    @Override
    public void deleteSampler(Sampler sampler) {
        this.deleteSampler0((SamplerImpl) sampler);
    }

    @Override
    public void deleteTexture(Texture texture) {
        this.deleteTexture0((TextureImpl) texture);
    }

    @Override
    public void deleteFence(Fence fence) {
        this.deleteFence0((FenceImpl) fence);
    }

    private void deleteFence0(FenceImpl fence) {
        fence.delete();
    }

    private void deleteTexture0(TextureImpl texture) {
        texture.delete();
    }

    private void deleteSampler0(SamplerImpl sampler) {
        sampler.delete();
    }

    private void deleteShader0(ShaderImpl shader) {
        shader.delete();
    }

    private void deleteProgram0(ProgramImpl<?> program) {
        program.delete();
    }

    private void deleteVertexArray0(VertexArrayImpl vertexArray) {
        this.stateTracker.notifyVertexArrayDeleted(vertexArray);

        int handle = vertexArray.handle();
        vertexArray.invalidateHandle();

        GL30C.glDeleteVertexArrays(handle);
    }

    private static class ImmediatePipelineCommandList implements PipelineCommandList {
        public ImmediatePipelineCommandList(RenderPipeline pipeline) {
            pipeline.enable();
        }

        @Override
        public <T> void useProgram(Program<T> program, ShaderEntrypoint<T> consumer) {
            consumer.accept(new ImmediateShaderCommandList((ProgramImpl<?>) program), program.getInterface());
        }
    }

    private static class ImmediateShaderCommandList implements ShaderCommandList {
        public ImmediateShaderCommandList(ProgramImpl<?> program) {
            program.bind();
        }

        @Override
        public void useTessellation(Tessellation tessellation, Consumer<TessellationCommandList> consumer) {
            consumer.accept(new ImmediateTessellationCommandList((TessellationImpl) tessellation));
        }
    }

    private static class ImmediateTessellationCommandList implements TessellationCommandList {
        private final TessellationImpl tessellation;

        public ImmediateTessellationCommandList(TessellationImpl tessellation) {
            this.tessellation = tessellation;
            this.tessellation.bind();
        }

        @Override
        public void multiDrawElementsBaseVertex(PointerBuffer pointer, IntBuffer count, IntBuffer baseVertex, IndexType indexType) {
            PrimitiveType primitiveType = this.tessellation.getPrimitiveType();
            GL32C.glMultiDrawElementsBaseVertex(primitiveType.getId(), count, indexType.getFormatId(), pointer, baseVertex);
        }
    }
}
