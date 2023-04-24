package me.jellysquid.mods.sodium.client.render.chunk;

import me.jellysquid.mods.sodium.client.gl.buffer.GlBuffer;
import me.jellysquid.mods.sodium.client.gl.buffer.GlBufferMapFlags;
import me.jellysquid.mods.sodium.client.gl.buffer.GlBufferUsage;
import me.jellysquid.mods.sodium.client.gl.buffer.GlMutableBuffer;
import me.jellysquid.mods.sodium.client.gl.device.CommandList;
import me.jellysquid.mods.sodium.client.gl.tessellation.GlIndexType;
import me.jellysquid.mods.sodium.client.gl.util.EnumBitField;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;

public class SharedQuadIndexBuffer {
    private static final int ELEMENTS_PER_PRIMITIVE = 6;
    private static final int VERTICES_PER_PRIMITIVE = 4;

    private final GlMutableBuffer buffer;
    private final IndexType indexType;

    private int maxPrimitives;

    public SharedQuadIndexBuffer(CommandList commandList, IndexType indexType) {
        this.buffer = commandList.createMutableBuffer();
        this.indexType = indexType;
    }

    static IndexType getSmallestIndexType(int vertexCount) {
        for (var type : IndexType.VALUES) {
            if (vertexCount <= type.getMaxVertexCount()) {
                return type;
            }
        }

        throw new IllegalArgumentException("No index type can represent %s vertices".formatted(vertexCount));
    }

    public void ensureCapacity(CommandList commandList, int vertexCount) {
        if (vertexCount > this.indexType.getMaxVertexCount()) {
            throw new IllegalArgumentException("Tried to reserve storage for more vertices in this buffer than it can hold");
        }

        int primitiveCount = vertexCount / VERTICES_PER_PRIMITIVE;

        if (primitiveCount > this.maxPrimitives) {
            this.grow(commandList, this.getNextSize(primitiveCount));
        }
    }

    private int getNextSize(int primitiveCount) {
        return Math.min(Math.max(this.maxPrimitives * 2, primitiveCount + 16384), this.indexType.getMaxPrimitiveCount());
    }

    private void grow(CommandList commandList, int primitiveCount) {
        var bufferSize = primitiveCount * this.indexType.getBytesPerElement() * ELEMENTS_PER_PRIMITIVE;

        commandList.allocateStorage(this.buffer, bufferSize, GlBufferUsage.STATIC_DRAW);

        var mapped = commandList.mapBuffer(this.buffer, 0, bufferSize, EnumBitField.of(GlBufferMapFlags.INVALIDATE_BUFFER, GlBufferMapFlags.WRITE, GlBufferMapFlags.UNSYNCHRONIZED));
        this.indexType.createIndexBuffer(mapped.getMemoryBuffer(), primitiveCount);

        commandList.unmap(mapped);

        this.maxPrimitives = primitiveCount;
    }


    public GlBuffer getBufferObject() {
        return this.buffer;
    }

    public void delete(CommandList commandList) {
        commandList.deleteBuffer(this.buffer);
    }

    public GlIndexType getIndexFormat() {
        return this.indexType.getFormat();
    }

    public IndexType getIndexType() {
        return this.indexType;
    }

    public enum IndexType {
        SHORT(GlIndexType.UNSIGNED_SHORT, 64 * 1024) {
            @Override
            public void createIndexBuffer(ByteBuffer byteBuffer, int primitiveCount) {
                ShortBuffer shortBuffer = byteBuffer.asShortBuffer();

                for (int primitiveIndex = 0; primitiveIndex < primitiveCount; primitiveIndex++) {
                    int indexOffset = primitiveIndex * ELEMENTS_PER_PRIMITIVE;
                    int vertexOffset = primitiveIndex * VERTICES_PER_PRIMITIVE;

                    shortBuffer.put(indexOffset + 0, (short) (vertexOffset + 0));
                    shortBuffer.put(indexOffset + 1, (short) (vertexOffset + 1));
                    shortBuffer.put(indexOffset + 2, (short) (vertexOffset + 2));

                    shortBuffer.put(indexOffset + 3, (short) (vertexOffset + 2));
                    shortBuffer.put(indexOffset + 4, (short) (vertexOffset + 3));
                    shortBuffer.put(indexOffset + 5, (short) (vertexOffset + 0));
                }
            }
        },
        INTEGER(GlIndexType.UNSIGNED_INT, Integer.MAX_VALUE) {
            @Override
            public void createIndexBuffer(ByteBuffer byteBuffer, int primitiveCount) {
                IntBuffer intBuffer = byteBuffer.asIntBuffer();

                for (int primitiveIndex = 0; primitiveIndex < primitiveCount; primitiveIndex++) {
                    int indexOffset = primitiveIndex * ELEMENTS_PER_PRIMITIVE;
                    int vertexOffset = primitiveIndex * VERTICES_PER_PRIMITIVE;

                    intBuffer.put(indexOffset + 0, vertexOffset + 0);
                    intBuffer.put(indexOffset + 1, vertexOffset + 1);
                    intBuffer.put(indexOffset + 2, vertexOffset + 2);

                    intBuffer.put(indexOffset + 3, vertexOffset + 2);
                    intBuffer.put(indexOffset + 4, vertexOffset + 3);
                    intBuffer.put(indexOffset + 5, vertexOffset + 0);
                }
            }
        };

        public static final IndexType[] VALUES = IndexType.values();

        private final GlIndexType format;
        private final int maxVertexCount;

        IndexType(GlIndexType format, int maxVertexCount) {
            this.format = format;
            this.maxVertexCount = maxVertexCount;
        }

        public abstract void createIndexBuffer(ByteBuffer buffer, int primitiveCount);

        public int getBytesPerElement() {
            return this.format.getStride();
        }

        public GlIndexType getFormat() {
            return this.format;
        }

        public int getMaxPrimitiveCount() {
            return this.maxVertexCount / 4;
        }

        public int getMaxVertexCount() {
            return this.maxVertexCount;
        }
    }
}
