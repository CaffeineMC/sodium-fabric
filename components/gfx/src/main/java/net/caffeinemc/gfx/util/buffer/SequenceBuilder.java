package net.caffeinemc.gfx.util.buffer;

import net.caffeinemc.gfx.api.types.ElementFormat;
import org.lwjgl.system.MemoryUtil;

public abstract class SequenceBuilder {
    public static final SequenceBuilder QUADS_INT = new SequenceBuilder(4, 6, ElementFormat.UNSIGNED_INT) {
        @Override
        public void write(long pointer, int baseVertex) {
            MemoryUtil.memPutInt(pointer + 0,  baseVertex + 0);
            MemoryUtil.memPutInt(pointer + 4,  baseVertex + 1);
            MemoryUtil.memPutInt(pointer + 8,  baseVertex + 2);
            MemoryUtil.memPutInt(pointer + 12, baseVertex + 2);
            MemoryUtil.memPutInt(pointer + 16, baseVertex + 3);
            MemoryUtil.memPutInt(pointer + 20, baseVertex + 0);
        }
    };

    public static final SequenceBuilder LINES_INT = new SequenceBuilder(4, 6, ElementFormat.UNSIGNED_INT) {
        @Override
        public void write(long pointer, int baseVertex) {
            MemoryUtil.memPutInt(pointer + 0,  baseVertex + 0);
            MemoryUtil.memPutInt(pointer + 4,  baseVertex + 1);
            MemoryUtil.memPutInt(pointer + 8,  baseVertex + 2);
            MemoryUtil.memPutInt(pointer + 12, baseVertex + 3);
            MemoryUtil.memPutInt(pointer + 16, baseVertex + 2);
            MemoryUtil.memPutInt(pointer + 20, baseVertex + 1);
        }
    };

    public static final SequenceBuilder DEFAULT_INT = new SequenceBuilder(1, 1, ElementFormat.UNSIGNED_INT) {
        @Override
        public void write(long pointer, int baseVertex) {
            MemoryUtil.memPutInt(pointer, baseVertex);
        }
    };

    public static final SequenceBuilder QUADS_SHORT = new SequenceBuilder(4, 6, ElementFormat.UNSIGNED_SHORT) {
        @Override
        public void write(long pointer, int baseVertex) {
            MemoryUtil.memPutShort(pointer + 0,  (short) (baseVertex + 0));
            MemoryUtil.memPutShort(pointer + 2,  (short) (baseVertex + 1));
            MemoryUtil.memPutShort(pointer + 4,  (short) (baseVertex + 2));
            MemoryUtil.memPutShort(pointer + 6,  (short) (baseVertex + 2));
            MemoryUtil.memPutShort(pointer + 8,  (short) (baseVertex + 3));
            MemoryUtil.memPutShort(pointer + 10, (short) (baseVertex + 0));
        }
    };

    public static final SequenceBuilder LINES_SHORT = new SequenceBuilder(4, 6, ElementFormat.UNSIGNED_SHORT) {
        @Override
        public void write(long pointer, int baseVertex) {
            MemoryUtil.memPutShort(pointer + 0,  (short) (baseVertex + 0));
            MemoryUtil.memPutShort(pointer + 2,  (short) (baseVertex + 1));
            MemoryUtil.memPutShort(pointer + 4,  (short) (baseVertex + 2));
            MemoryUtil.memPutShort(pointer + 6,  (short) (baseVertex + 3));
            MemoryUtil.memPutShort(pointer + 8,  (short) (baseVertex + 2));
            MemoryUtil.memPutShort(pointer + 10, (short) (baseVertex + 1));
        }
    };

    public static final SequenceBuilder DEFAULT_SHORT = new SequenceBuilder(1, 1, ElementFormat.UNSIGNED_SHORT) {
        @Override
        public void write(long pointer, int baseVertex) {
            MemoryUtil.memPutShort(pointer, (short) baseVertex);
        }
    };

    public static final SequenceBuilder QUADS_BYTE = new SequenceBuilder(4, 6, ElementFormat.UNSIGNED_BYTE) {
        @Override
        public void write(long pointer, int baseVertex) {
            MemoryUtil.memPutByte(pointer + 0, (byte) (baseVertex + 0));
            MemoryUtil.memPutByte(pointer + 1, (byte) (baseVertex + 1));
            MemoryUtil.memPutByte(pointer + 2, (byte) (baseVertex + 2));
            MemoryUtil.memPutByte(pointer + 3, (byte) (baseVertex + 2));
            MemoryUtil.memPutByte(pointer + 4, (byte) (baseVertex + 3));
            MemoryUtil.memPutByte(pointer + 5, (byte) (baseVertex + 0));
        }
    };

    public static final SequenceBuilder LINES_BYTE = new SequenceBuilder(4, 6, ElementFormat.UNSIGNED_BYTE) {
        @Override
        public void write(long pointer, int baseVertex) {
            MemoryUtil.memPutByte(pointer + 0, (byte) (baseVertex + 0));
            MemoryUtil.memPutByte(pointer + 1, (byte) (baseVertex + 1));
            MemoryUtil.memPutByte(pointer + 2, (byte) (baseVertex + 2));
            MemoryUtil.memPutByte(pointer + 3, (byte) (baseVertex + 3));
            MemoryUtil.memPutByte(pointer + 4, (byte) (baseVertex + 2));
            MemoryUtil.memPutByte(pointer + 5, (byte) (baseVertex + 1));
        }
    };

    public static final SequenceBuilder DEFAULT_BYTE = new SequenceBuilder(1, 1, ElementFormat.UNSIGNED_BYTE) {
        @Override
        public void write(long pointer, int baseVertex) {
            MemoryUtil.memPutByte(pointer, (byte) baseVertex);
        }
    };

    private final int verticesPerPrimitive;
    private final int indicesPerPrimitive;
    private final ElementFormat elementFormat;

    protected SequenceBuilder(int verticesPerPrimitive, int indicesPerPrimitive, ElementFormat elementFormat) {
        this.verticesPerPrimitive = verticesPerPrimitive;
        this.indicesPerPrimitive = indicesPerPrimitive;
        this.elementFormat = elementFormat;
    }

    public abstract void write(long pointer, int baseVertex);

    public int getVerticesPerPrimitive() {
        return this.verticesPerPrimitive;
    }

    public int getIndicesPerPrimitive() {
        return this.indicesPerPrimitive;
    }

    public ElementFormat getElementFormat() {
        return this.elementFormat;
    }
}
