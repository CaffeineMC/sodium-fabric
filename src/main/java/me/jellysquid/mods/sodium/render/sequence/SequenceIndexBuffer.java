package me.jellysquid.mods.sodium.render.sequence;

import me.jellysquid.mods.sodium.opengl.buffer.Buffer;
import me.jellysquid.mods.sodium.opengl.device.RenderDevice;
import me.jellysquid.mods.sodium.opengl.types.IntType;

public class SequenceIndexBuffer {
    private final RenderDevice device;
    private final SequenceBuilder builder;

    private Buffer buffer;
    private int maxVertices;

    public SequenceIndexBuffer(RenderDevice device, SequenceBuilder builder) {
        this.device = device;
        this.builder = builder;
    }

    public void ensureCapacity(int vertexCount) {
        if (vertexCount > this.maxVertices) {
            this.grow(this.getNextSize(vertexCount));
        }
    }

    private int getNextSize(int vertexCount) {
        return Math.max(this.maxVertices * 2, vertexCount + 2048);
    }

    private void grow(int vertexCount) {
        this.delete();

        var verticesPerPrimitive = this.builder.getVerticesPerPrimitive();
        var indicesPerPrimitive = this.builder.getIndicesPerPrimitive();

        var primitiveCount = vertexCount / verticesPerPrimitive;
        var bufferSize = (long) indicesPerPrimitive * primitiveCount * 4L;

        this.buffer = this.device.createBuffer(bufferSize, (buffer) -> {
            var intBuffer = buffer.asIntBuffer();

            for (int primitiveIndex = 0; primitiveIndex < primitiveCount; primitiveIndex++) {
                this.builder.write(intBuffer, primitiveIndex * verticesPerPrimitive);
            }
        });

        this.maxVertices = vertexCount;
    }

    public Buffer getBuffer() {
        return this.buffer;
    }

    public void delete() {
        if (this.buffer != null) {
            this.device.deleteBuffer(this.buffer);
            this.buffer = null;
        }
    }

    public IntType getElementDataType() {
        return IntType.UNSIGNED_INT;
    }
}
