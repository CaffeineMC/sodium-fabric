package me.jellysquid.mods.sodium.client.render.immediate;

import me.jellysquid.mods.sodium.client.gl.buffer.GlBuffer;
import me.jellysquid.mods.sodium.client.gl.device.CommandList;

public class SequenceIndexBuffer {
    private final SequenceBuilder builder;
    private GlBuffer buffer;

    private int maxVertices;

    public SequenceIndexBuffer(SequenceBuilder builder) {
        this.builder = builder;
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
        this.delete(commandList);

        var verticesPerPrimitive = this.builder.getVerticesPerPrimitive();
        var indicesPerPrimitive = this.builder.getIndicesPerPrimitive();

        var primitiveCount = vertexCount / verticesPerPrimitive;
        var bufferSize = (long) indicesPerPrimitive * primitiveCount * 4L;

        this.buffer = commandList.createBuffer(bufferSize, (buffer) -> {
            var intBuffer = buffer.asIntBuffer();

            for (int primitiveIndex = 0; primitiveIndex < primitiveCount; primitiveIndex++) {
                this.builder.write(intBuffer, primitiveIndex * verticesPerPrimitive);
            }
        });

        this.maxVertices = vertexCount;
    }

    public GlBuffer getBuffer() {
        return this.buffer;
    }

    public void delete(CommandList commandList) {
        if (this.buffer != null) {
            commandList.deleteBuffer(this.buffer);
            this.buffer = null;
        }
    }
}
