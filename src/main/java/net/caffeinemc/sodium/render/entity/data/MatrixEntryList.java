package net.caffeinemc.sodium.render.entity.data;

import net.caffeinemc.sodium.interop.vanilla.math.matrix.MatrixUtil;
import net.minecraft.client.util.math.MatrixStack;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.Arrays;

public class MatrixEntryList {
    private static final int DEFAULT_SIZE = 16;
    public static final int ENTRY_SIZE_BYTES = 16 * Float.BYTES + 12 * Float.BYTES;

    private static final int EMPTY_ID = -1;

    private MatrixStack.Entry[] elementArray;
    private boolean[] elementWrittenArray; // needed to track null entries
    private int largestPartId = EMPTY_ID; // needed to write correct amount to buffer

    public MatrixEntryList() {
        this.elementArray = new MatrixStack.Entry[DEFAULT_SIZE];
        this.elementWrittenArray = new boolean[DEFAULT_SIZE];
    }

    public MatrixEntryList(int initialPartId) {
        int size;
        if (initialPartId > DEFAULT_SIZE) {
            size = initialPartId;
            size |= (size >> 16);
            size |= (size >> 8);
            size |= (size >> 4);
            size |= (size >> 2);
            size |= (size >> 1);
            size++;
        } else {
            size = DEFAULT_SIZE;
        }

        this.elementArray = new MatrixStack.Entry[size];
        this.elementWrittenArray = new boolean[size];
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

        if (matrixCount * ENTRY_SIZE_BYTES > buffer.remaining()) {
            throw new IndexOutOfBoundsException("Matrix entries could not be written to buffer, not enough room");
        }

        // USED TO BE idx < this.elementArray.length
        for (int idx = 0; idx < matrixCount; idx++) {
            if (this.elementWrittenArray[idx]) {
                MatrixStack.Entry matrixEntry = this.elementArray[idx];
                if (matrixEntry != null) {
                    writeMatrixEntry(buffer, matrixEntry);
                } else {
                    writeNullEntry(buffer);
                }
            } else {
                writeMatrixEntry(buffer, baseMatrixEntry);
            }
        }
    }

    private static void writeMatrixEntry(ByteBuffer buffer, MatrixStack.Entry matrixEntry) {
        // TODO: if this is slow, consider re-implementing the direct set functions in the extended versions of the matrices.
        FloatBuffer floatBuffer = buffer.asFloatBuffer();

        // mat4 ModelViewMat
        matrixEntry.getPositionMatrix().writeColumnMajor(floatBuffer);
        // mat3x4 NormalMat
        MatrixUtil.getExtendedMatrix(matrixEntry.getNormalMatrix()).writeColumnMajor3x4(floatBuffer);
    }

    private static void writeNullEntry(ByteBuffer buffer) {
        MemoryUtil.memSet(MemoryUtil.memAddress(buffer), 0, ENTRY_SIZE_BYTES);
        buffer.position(buffer.position() + ENTRY_SIZE_BYTES);
    }
}
