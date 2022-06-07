package net.caffeinemc.sodium.render.entity.data;

import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Matrix3f;
import net.minecraft.util.math.Matrix4f;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.util.Arrays;

public class MatrixEntryList {
    private static final int DEFAULT_SIZE = 16;
    public static final int STRUCT_SIZE_BYTES = 16 * Float.BYTES + 12 * Float.BYTES;

    private static final int EMPTY_ID = -1;

    private MatrixStack.Entry[] elementArray;
    private boolean[] elementWrittenArray; // needed to track null entries
    private int largestPartId = EMPTY_ID; // needed to write correct amount to buffer

    public MatrixEntryList() {
        this.elementArray = new MatrixStack.Entry[DEFAULT_SIZE];
        this.elementWrittenArray = new boolean[DEFAULT_SIZE];
    }

    public MatrixEntryList(int initialSize) {
        this.elementArray = new MatrixStack.Entry[initialSize];
        this.elementWrittenArray = new boolean[initialSize];
    }

    public void set(int partId, MatrixStack.Entry element) {
        if (partId > this.largestPartId) {
            this.largestPartId = partId;

            if (partId >= this.elementArray.length) {
                // expand the array to the closest power of 2 that will fit the partId
                int newSize = partId;
                newSize |= (newSize >> 16);
                newSize |= (newSize >> 8);
                newSize |= (newSize >> 4);
                newSize |= (newSize >> 2);
                newSize |= (newSize >> 1);
                newSize++;
                this.elementWrittenArray = Arrays.copyOf(this.elementWrittenArray, newSize);
                this.elementArray = Arrays.copyOf(this.elementArray, newSize);
            }
        }
        this.elementWrittenArray[partId] = true;
        this.elementArray[partId] = element;
    }

    public boolean getElementWritten(int partId) {
        return this.elementWrittenArray[partId];
    }

    public MatrixStack.Entry get(int partId) {
        return this.elementArray[partId];
    }

    public void clear() {
        // only fill modified portions of arrays
        if (this.largestPartId > EMPTY_ID) {
            Arrays.fill(this.elementArray, 0, this.largestPartId, null);
            Arrays.fill(this.elementWrittenArray, 0, this.largestPartId, false);
            this.largestPartId = EMPTY_ID;
        }
    }

    /**
     * @return the largest part id added since the last clear, or -1 if nothing has been added
     */
    public int getLargestPartId() {
        return this.largestPartId;
    }

    public boolean isEmpty() {
        return this.largestPartId == EMPTY_ID;
    }

    /**
     * Writes the contents of this list to a buffer, with null entries represented
     * as a stream of 0s, and unwritten elements represented as the base entry.
     *
     * @param buffer the buffer to write to.
     */
    public void writeToBuffer(ByteBuffer buffer, MatrixStack.Entry baseMatrixEntry) {
        int matrixCount = this.largestPartId + 1;

        if (matrixCount * STRUCT_SIZE_BYTES > buffer.remaining()) {
            throw new IndexOutOfBoundsException("Matrix entries could not be written to buffer, not enough room");
        }

        long pointer = MemoryUtil.memAddress(buffer);
        // USED TO BE idx < this.elementArray.length
        for (int idx = 0; idx < matrixCount; idx++) {
            if (this.elementWrittenArray[idx]) {
                MatrixStack.Entry matrixEntry = this.elementArray[idx];
                if (matrixEntry != null) {
                    writeMatrixEntry(pointer + (long) idx * STRUCT_SIZE_BYTES, matrixEntry);
                } else {
                    writeNullEntry(pointer + (long) idx * STRUCT_SIZE_BYTES);
                }
            } else {
                writeMatrixEntry(pointer + (long) idx * STRUCT_SIZE_BYTES, baseMatrixEntry);
            }
        }
        buffer.position(buffer.position() + (matrixCount * STRUCT_SIZE_BYTES));
    }

    private static void writeMatrixEntry(long pointer, MatrixStack.Entry matrixEntry) {
        Matrix4f model = matrixEntry.getPositionMatrix();
        MemoryUtil.memPutFloat(pointer, model.a00);
        MemoryUtil.memPutFloat(pointer + 4, model.a10);
        MemoryUtil.memPutFloat(pointer + 8, model.a20);
        MemoryUtil.memPutFloat(pointer + 12, model.a30);
        MemoryUtil.memPutFloat(pointer + 16, model.a01);
        MemoryUtil.memPutFloat(pointer + 20, model.a11);
        MemoryUtil.memPutFloat(pointer + 24, model.a21);
        MemoryUtil.memPutFloat(pointer + 28, model.a31);
        MemoryUtil.memPutFloat(pointer + 32, model.a02);
        MemoryUtil.memPutFloat(pointer + 36, model.a12);
        MemoryUtil.memPutFloat(pointer + 40, model.a22);
        MemoryUtil.memPutFloat(pointer + 44, model.a32);
        MemoryUtil.memPutFloat(pointer + 48, model.a03);
        MemoryUtil.memPutFloat(pointer + 52, model.a13);
        MemoryUtil.memPutFloat(pointer + 56, model.a23);
        MemoryUtil.memPutFloat(pointer + 60, model.a33);

        Matrix3f normal = matrixEntry.getNormalMatrix();
        MemoryUtil.memPutFloat(pointer + 64, normal.a00);
        MemoryUtil.memPutFloat(pointer + 68, normal.a10);
        MemoryUtil.memPutFloat(pointer + 72, normal.a20);
        // padding
        MemoryUtil.memPutFloat(pointer + 80, normal.a01);
        MemoryUtil.memPutFloat(pointer + 84, normal.a11);
        MemoryUtil.memPutFloat(pointer + 88, normal.a21);
        // padding
        MemoryUtil.memPutFloat(pointer + 96, normal.a02);
        MemoryUtil.memPutFloat(pointer + 100, normal.a12);
        MemoryUtil.memPutFloat(pointer + 104, normal.a22);
        // padding
    }

    private static void writeNullEntry(long pointer) {
        MemoryUtil.memSet(pointer, 0, STRUCT_SIZE_BYTES);
    }
}
