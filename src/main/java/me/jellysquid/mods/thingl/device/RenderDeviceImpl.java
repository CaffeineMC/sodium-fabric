package me.jellysquid.mods.thingl.device;

import me.jellysquid.mods.thingl.array.GlVertexArray;
import me.jellysquid.mods.thingl.buffer.*;
import me.jellysquid.mods.thingl.functions.DeviceFunctions;
import me.jellysquid.mods.thingl.lists.PipelineCommandList;
import me.jellysquid.mods.thingl.lists.ShaderCommandList;
import me.jellysquid.mods.thingl.lists.TessellationCommandList;
import me.jellysquid.mods.thingl.pipeline.RenderPipeline;
import me.jellysquid.mods.thingl.shader.GlProgram;
import me.jellysquid.mods.thingl.shader.GlShader;
import me.jellysquid.mods.thingl.shader.ShaderBindingContext;
import me.jellysquid.mods.thingl.shader.ShaderType;
import me.jellysquid.mods.thingl.state.StateTracker;
import me.jellysquid.mods.thingl.sync.GlFence;
import me.jellysquid.mods.thingl.tessellation.GlIndexType;
import me.jellysquid.mods.thingl.tessellation.GlPrimitiveType;
import me.jellysquid.mods.thingl.tessellation.GlTessellation;
import me.jellysquid.mods.thingl.tessellation.TessellationBinding;
import me.jellysquid.mods.thingl.texture.GlSampler;
import me.jellysquid.mods.thingl.texture.GlTexture;
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
    public GlShader createShader(ShaderType type, String source) {
        return new GlShader(this, type, source);
    }

    @Override
    public <T> GlProgram<T> createProgram(GlShader[] shaders, Function<ShaderBindingContext, T> interfaceFactory) {
        return new GlProgram<>(this, shaders, interfaceFactory);
    }

    @Override
    public GlTessellation createTessellation(GlPrimitiveType primitiveType, TessellationBinding[] bindings) {
        return new GlTessellation(this, primitiveType, bindings);
    }

    @Override
    public GlMutableBuffer createMutableBuffer() {
        return new GlMutableBuffer(this);
    }

    @Override
    public GlImmutableBuffer createImmutableBuffer(long bufferSize, EnumBitField<GlBufferStorageFlags> flags) {
        GlImmutableBuffer buffer = new GlImmutableBuffer(this, flags);

        buffer.bind(GlBufferTarget.ARRAY_BUFFER);
        RenderDeviceImpl.this.functions.getBufferStorageFunctions()
                .createBufferStorage(GlBufferTarget.ARRAY_BUFFER, bufferSize, flags);

        return buffer;
    }

    @Override
    public GlSampler createSampler() {
        return new GlSampler(this);
    }

    @Override
    public void uploadData(GlMutableBuffer glBuffer, ByteBuffer byteBuffer, GlBufferUsage usage) {
        glBuffer.bind(GlBufferTarget.ARRAY_BUFFER);

        GL20C.glBufferData(GlBufferTarget.ARRAY_BUFFER.getTargetParameter(), byteBuffer, usage.getId());
        glBuffer.setSize(byteBuffer.remaining());
    }


    @Override
    public void copyBufferSubData(GlBuffer src, GlBuffer dst, long readOffset, long writeOffset, long bytes) {
        src.bind(GlBufferTarget.COPY_READ_BUFFER);
        dst.bind(GlBufferTarget.COPY_WRITE_BUFFER);

        GL31C.glCopyBufferSubData(GL31C.GL_COPY_READ_BUFFER, GL31C.GL_COPY_WRITE_BUFFER, readOffset, writeOffset, bytes);
    }


    @Override
    public void deleteTessellation(GlTessellation tessellation) {
        tessellation.delete();
    }

    @Override
    public GlBufferMapping mapBuffer(GlBuffer buffer, long offset, long length, EnumBitField<GlBufferMapFlags> flags) {
        if (buffer.getActiveMapping() != null) {
            throw new IllegalStateException("Buffer is already mapped");
        }

        if (flags.contains(GlBufferMapFlags.PERSISTENT) && !(buffer instanceof GlImmutableBuffer)) {
            throw new IllegalStateException("Tried to map mutable buffer as persistent");
        }

        // TODO: speed this up?
        if (buffer instanceof GlImmutableBuffer) {
            EnumBitField<GlBufferStorageFlags> bufferFlags = ((GlImmutableBuffer) buffer).getFlags();

            if (flags.contains(GlBufferMapFlags.PERSISTENT) && !bufferFlags.contains(GlBufferStorageFlags.PERSISTENT)) {
                throw new IllegalArgumentException("Tried to map non-persistent buffer as persistent");
            }

            if (flags.contains(GlBufferMapFlags.WRITE) && !bufferFlags.contains(GlBufferStorageFlags.MAP_WRITE)) {
                throw new IllegalStateException("Tried to map non-writable buffer as writable");
            }

            if (flags.contains(GlBufferMapFlags.READ) && !bufferFlags.contains(GlBufferStorageFlags.MAP_READ)) {
                throw new IllegalStateException("Tried to map non-readable buffer as readable");
            }
        }

        buffer.bind(GlBufferTarget.ARRAY_BUFFER);

        ByteBuffer buf = GL32C.glMapBufferRange(GlBufferTarget.ARRAY_BUFFER.getTargetParameter(), offset, length, flags.getBitField());

        if (buf == null) {
            throw new RuntimeException("Failed to map buffer");
        }

        GlBufferMapping mapping = new GlBufferMapping(buffer, buf);

        buffer.setActiveMapping(mapping);

        return mapping;
    }

    @Override
    public void unmap(GlBufferMapping map) {
        map.checkDisposed();

        GlBuffer buffer = map.getBufferObject();

        buffer.bind(GlBufferTarget.ARRAY_BUFFER);
        GL32C.glUnmapBuffer(GlBufferTarget.ARRAY_BUFFER.getTargetParameter());

        buffer.setActiveMapping(null);
        map.dispose();
    }

    @Override
    public void flushMappedRange(GlBufferMapping map, int offset, int length) {
        map.checkDisposed();

        GlBuffer buffer = map.getBufferObject();

        buffer.bind(GlBufferTarget.COPY_READ_BUFFER);
        GL32C.glFlushMappedBufferRange(GlBufferTarget.COPY_READ_BUFFER.getTargetParameter(), offset, length);
    }

    @Override
    public GlFence createFence() {
        return new GlFence(GL32C.glFenceSync(GL32C.GL_SYNC_GPU_COMMANDS_COMPLETE, 0));
    }

    @Override
    public GlTexture createTexture() {
        return new GlTexture(this);
    }

    @Override
    public void allocateStorage(GlMutableBuffer buffer, long bufferSize, GlBufferUsage usage) {
        buffer.bind(GlBufferTarget.ARRAY_BUFFER);

        GL20C.glBufferData(GlBufferTarget.ARRAY_BUFFER.getTargetParameter(), bufferSize, usage.getId());
        buffer.setSize(bufferSize);
    }

    @Override
    public void deleteBuffer(GlBuffer buffer) {
        if (buffer.getActiveMapping() != null) {
            this.unmap(buffer.getActiveMapping());
        }

        this.stateTracker.notifyBufferDeleted(buffer);

        int handle = buffer.handle();
        buffer.invalidateHandle();

        GL20C.glDeleteBuffers(handle);
    }

    @Override
    public void deleteVertexArray(GlVertexArray vertexArray) {
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
        public <T> void useProgram(GlProgram<T> program, ShaderEntrypoint<T> consumer) {
            consumer.accept(new ImmediateShaderCommandList(program), program.getInterface());
        }
    }

    private static class ImmediateShaderCommandList implements ShaderCommandList {
        public ImmediateShaderCommandList(GlProgram<?> program) {
            program.bind();
        }

        @Override
        public void useTessellation(GlTessellation tessellation, Consumer<TessellationCommandList> consumer) {
            consumer.accept(new ImmediateTessellationCommandList(tessellation));
        }
    }

    private static class ImmediateTessellationCommandList implements TessellationCommandList {
        private final GlTessellation tessellation;

        public ImmediateTessellationCommandList(GlTessellation tessellation) {
            this.tessellation = tessellation;
            this.tessellation.bind();
        }

        @Override
        public void multiDrawElementsBaseVertex(PointerBuffer pointer, IntBuffer count, IntBuffer baseVertex, GlIndexType indexType) {
            GlPrimitiveType primitiveType = this.tessellation.getPrimitiveType();
            GL32C.glMultiDrawElementsBaseVertex(primitiveType.getId(), count, indexType.getFormatId(), pointer, baseVertex);
        }
    }
}
