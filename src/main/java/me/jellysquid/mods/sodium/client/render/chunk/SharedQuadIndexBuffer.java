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

public class SharedQuadIndexBuffer {
    public static final GlIndexType INDEX_TYPE = GlIndexType.UNSIGNED_INT;

    private static final int ELEMENTS_PER_PRIMITIVE = 6;
    private static final int VERTICES_PER_PRIMITIVE = 4;

    private final GlMutableBuffer indexBuffer;

    private int maxPrimitives;

    public SharedQuadIndexBuffer(CommandList commandList) {
        this.indexBuffer = commandList.createMutableBuffer();
    }

    public void ensureCapacity(CommandList commandList, int vertexCount) {
        int primitiveCount = vertexCount / VERTICES_PER_PRIMITIVE;

        if (primitiveCount > this.maxPrimitives) {
            this.grow(commandList, this.getNextSize(primitiveCount));
        }
    }

    private int getNextSize(int primitiveCount) {
        return Math.max(this.maxPrimitives * 2, primitiveCount + 16384);
    }

    private void grow(CommandList commandList, int primitiveCount) {
        var bufferSize = primitiveCount * Integer.BYTES * ELEMENTS_PER_PRIMITIVE;

        commandList.allocateStorage(this.indexBuffer, bufferSize, GlBufferUsage.STATIC_DRAW);

        var mapped = commandList.mapBuffer(this.indexBuffer, 0, bufferSize, EnumBitField.of(GlBufferMapFlags.INVALIDATE_BUFFER, GlBufferMapFlags.WRITE, GlBufferMapFlags.UNSYNCHRONIZED));
        writeElementBuffer(mapped.getMemoryBuffer(), primitiveCount);

        commandList.unmap(mapped);

        this.maxPrimitives = primitiveCount;
    }


    public GlBuffer getBufferObject() {
        return this.indexBuffer;
    }

    public void delete(CommandList commandList) {
        commandList.deleteBuffer(this.indexBuffer);
    }

    private static void writeElementBuffer(ByteBuffer byteBuffer, int primitiveCount) {
        IntBuffer intBuffer = byteBuffer.asIntBuffer();

        for (int primitiveIndex = 0; primitiveIndex < primitiveCount; primitiveIndex++) {
            int indexOffset = primitiveIndex * ELEMENTS_PER_PRIMITIVE;
            int vertexOffset = primitiveIndex * VERTICES_PER_PRIMITIVE;

            intBuffer.put(indexOffset + 0, (vertexOffset + 0));
            intBuffer.put(indexOffset + 1, (vertexOffset + 1));
            intBuffer.put(indexOffset + 2, (vertexOffset + 2));

            intBuffer.put(indexOffset + 3, (vertexOffset + 2));
            intBuffer.put(indexOffset + 4, (vertexOffset + 3));
            intBuffer.put(indexOffset + 5, (vertexOffset + 0));
        }
    }
}
