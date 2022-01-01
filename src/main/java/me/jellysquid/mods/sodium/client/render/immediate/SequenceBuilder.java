package me.jellysquid.mods.sodium.client.render.immediate;

import java.nio.IntBuffer;

public abstract class SequenceBuilder {
    public static final SequenceBuilder QUADS = new SequenceBuilder(4, 6) {
        @Override
        public void write(IntBuffer buffer, int baseVertex) {
            buffer.put(baseVertex + 0);
            buffer.put(baseVertex + 1);
            buffer.put(baseVertex + 2);
            buffer.put(baseVertex + 2);
            buffer.put(baseVertex + 3);
            buffer.put(baseVertex + 0);
        }
    };

    public static final SequenceBuilder LINES = new SequenceBuilder(4, 6) {
        @Override
        public void write(IntBuffer buffer, int baseVertex) {
            buffer.put(baseVertex + 0);
            buffer.put(baseVertex + 1);
            buffer.put(baseVertex + 2);
            buffer.put(baseVertex + 3);
            buffer.put(baseVertex + 2);
            buffer.put(baseVertex + 1);
        }
    };

    public static final SequenceBuilder NONE = new SequenceBuilder(1, 1) {
        @Override
        public void write(IntBuffer buffer, int baseVertex) {
            buffer.put(baseVertex);
        }
    };

    private final int verticesPerPrimitive;
    private final int indicesPerPrimitive;

    protected SequenceBuilder(int verticesPerPrimitive, int indicesPerPrimitive) {
        this.verticesPerPrimitive = verticesPerPrimitive;
        this.indicesPerPrimitive = indicesPerPrimitive;
    }

    public abstract void write(IntBuffer buffer, int baseVertex);

    public int getVerticesPerPrimitive() {
        return this.verticesPerPrimitive;
    }

    public int getIndicesPerPrimitive() {
        return this.indicesPerPrimitive;
    }
}
