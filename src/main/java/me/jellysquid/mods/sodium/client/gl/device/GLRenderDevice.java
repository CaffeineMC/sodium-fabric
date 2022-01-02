package me.jellysquid.mods.sodium.client.gl.device;

import me.jellysquid.mods.sodium.client.gl.buffer.*;
import me.jellysquid.mods.sodium.client.gl.sync.GlFence;
import me.jellysquid.mods.sodium.client.gl.array.VertexArray;
import me.jellysquid.mods.sodium.client.gl.array.VertexArrayCommandList;
import me.jellysquid.mods.sodium.client.gl.array.VertexArrayDescription;
import me.jellysquid.mods.sodium.client.gl.util.EnumBitField;
import org.lwjgl.opengl.GL20C;
import org.lwjgl.opengl.GL30C;
import org.lwjgl.opengl.GL32C;
import org.lwjgl.opengl.GL45C;

import java.nio.ByteBuffer;
import java.util.function.Consumer;

public class GLRenderDevice implements RenderDevice {
    @Override
    public CommandList createCommandList() {
        return new ImmediateCommandList();
    }

    private static class ImmediateCommandList implements CommandList {
        @Override
        public void copyBufferSubData(GlBuffer src, GlBuffer dst, long readOffset, long writeOffset, long bytes) {
            GL45C.glCopyNamedBufferSubData(src.handle(), dst.handle(), readOffset, writeOffset, bytes);
        }

        @Override
        public void deleteBuffer(GlBuffer buffer) {
            int handle = buffer.handle();
            buffer.invalidateHandle();

            GL20C.glDeleteBuffers(handle);
        }

        @Override
        public void flush() {
            // NO-OP
        }

        @Override
        public GlFence createFence() {
            return new GlFence(GL32C.glFenceSync(GL32C.GL_SYNC_GPU_COMMANDS_COMPLETE, 0));
        }

        @Override
        public GlBuffer createBuffer(ByteBuffer data, EnumBitField<GlBufferStorageFlags> flags) {
            return this.createBuffer(data.remaining(), (writer) -> {
                writer.put(data.asReadOnlyBuffer());
            }, flags);
        }

        @Override
        public GlBuffer createBuffer(long capacity, EnumBitField<GlBufferStorageFlags> flags) {
            var handle = GL45C.glCreateBuffers();
            GL45C.glNamedBufferStorage(handle, capacity, flags.getBitField());

            return new GlBuffer(capacity, handle);
        }

        @Override
        public GlBuffer createBuffer(long capacity, Consumer<ByteBuffer> builder, EnumBitField<GlBufferStorageFlags> flags) {
            var handle = GL45C.glCreateBuffers();
            GL45C.glNamedBufferStorage(handle, capacity, flags.getBitField() | GL45C.GL_MAP_WRITE_BIT);

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
        public GlMappedBuffer createMappedBuffer(long capacity, EnumBitField<GlBufferStorageFlags> storageFlags, EnumBitField<GlBufferMapFlags> mapFlags) {
            var handle = GL45C.glCreateBuffers();
            GL45C.glNamedBufferStorage(handle, capacity, storageFlags.getBitField());

            ByteBuffer data = GL45C.glMapNamedBufferRange(handle, 0, capacity, mapFlags.getBitField());

            if (data == null) {
                throw new RuntimeException("Failed to map buffer");
            }

            return new GlMappedBuffer(capacity, handle, data);
        }

        @Override
        public <T extends Enum<T>> VertexArray<T> createVertexArray(VertexArrayDescription<T> desc) {
            return new VertexArray<>(GL45C.glCreateVertexArrays(), desc);
        }

        @Override
        public <T extends Enum<T>> void useVertexArray(VertexArray<T> array, Consumer<VertexArrayCommandList<T>> consumer) {
            int prev = GL30C.glGetInteger(GL30C.GL_VERTEX_ARRAY_BINDING);
            GL30C.glBindVertexArray(array.handle());
            consumer.accept(new ImmediateVertexArrayCommandList<>(array));
            GL30C.glBindVertexArray(prev);
        }

        @Override
        public void deleteVertexArray(VertexArray<?> array) {
            GL30C.glDeleteVertexArrays(array.handle());
            array.invalidateHandle();
        }
    }
}
