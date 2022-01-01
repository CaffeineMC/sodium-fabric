package me.jellysquid.mods.sodium.client.render.immediate;

import me.jellysquid.mods.sodium.client.gl.buffer.GlBuffer;
import me.jellysquid.mods.sodium.client.gl.buffer.GlBufferMapFlags;
import me.jellysquid.mods.sodium.client.gl.buffer.GlBufferUsage;
import me.jellysquid.mods.sodium.client.gl.buffer.GlMutableBuffer;
import me.jellysquid.mods.sodium.client.gl.device.CommandList;
import me.jellysquid.mods.sodium.client.gl.util.EnumBitField;

public class SequenceIndexBuffer {
    private final SequenceBuilder builder;
    private final GlMutableBuffer buffer;

    private int maxVertices;

    public SequenceIndexBuffer(CommandList commandList, SequenceBuilder builder) {
        this.builder = builder;
        this.buffer = commandList.createMutableBuffer();
    }

    public void ensureCapacity(CommandList commandList, int vertexCount) {
        if (vertexCount > this.maxVertices) {
            this.grow(commandList, this.getNextSize(vertexCount));
        }
    }

    private int getNextSize(int vertexCount) {
        return Math.max(this.maxVertices * 2, vertexCount + 2048);
    }

    private void grow(CommandList commandList, int vertexCount) {
        var verticesPerPrimitive = this.builder.getVerticesPerPrimitive();
        var indicesPerPrimitive = this.builder.getIndicesPerPrimitive();

        var primitiveCount = vertexCount / verticesPerPrimitive;
        var bufferSize = (long) indicesPerPrimitive * primitiveCount * 4L;

        commandList.allocateStorage(this.buffer, bufferSize, GlBufferUsage.STATIC_DRAW);

        var mapping = commandList.mapBuffer(this.buffer, 0, bufferSize,
                EnumBitField.of(GlBufferMapFlags.INVALIDATE_BUFFER, GlBufferMapFlags.WRITE));

        var intBuffer = mapping.getMemoryBuffer()
                .asIntBuffer();

        for (int primitiveIndex = 0; primitiveIndex < primitiveCount; primitiveIndex++) {
            this.builder.write(intBuffer, primitiveIndex * verticesPerPrimitive);
        }

        commandList.unmap(mapping);

        this.maxVertices = vertexCount;
    }

    public GlBuffer getBuffer() {
        return this.buffer;
    }

    public void delete(CommandList commandList) {
        commandList.deleteBuffer(this.buffer);
    }
}
