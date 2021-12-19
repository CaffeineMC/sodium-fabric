package me.jellysquid.mods.sodium.render.entity.data;

import java.util.Arrays;

import me.jellysquid.mods.sodium.render.entity.BakedModelUtils;
import me.jellysquid.mods.sodium.render.entity.buffer.SectionedPersistentBuffer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Matrix3f;
import net.minecraft.util.math.Matrix4f;
import org.lwjgl.system.MemoryUtil;

public class MatrixEntryList {
    private static final int DEFAULT_SIZE = 16;

    private MatrixStack.Entry[] elementArray;
    private boolean[] elementWrittenArray; // needed to track of null entries
    private int largestPartId = -1; // needed to write correct amount to buffer

    public MatrixEntryList() {
        elementArray = new MatrixStack.Entry[DEFAULT_SIZE];
        elementWrittenArray = new boolean[DEFAULT_SIZE];
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

        elementArray = new MatrixStack.Entry[size];
        elementWrittenArray = new boolean[size];
    }

    public void set(int partId, MatrixStack.Entry element) {
        if (partId > largestPartId) {
            largestPartId = partId;

            if (partId >= elementArray.length) {
                // expand the array to the closest power of 2 that will fit the partId
                int newSize = partId;
                newSize |= (newSize >> 16);
                newSize |= (newSize >> 8);
                newSize |= (newSize >> 4);
                newSize |= (newSize >> 2);
                newSize |= (newSize >> 1);
                newSize++;
                elementWrittenArray = Arrays.copyOf(elementWrittenArray, newSize);
                elementArray = Arrays.copyOf(elementArray, newSize);
            }
        }
        elementWrittenArray[partId] = true;
        elementArray[partId] = element;
    }

    public boolean getElementWritten(int partId) {
        return elementWrittenArray[partId];
    }

    public MatrixStack.Entry get(int partId) {
        return elementArray[partId];
    }

    public void clear() {
        // only fill modified portions of arrays
        Arrays.fill(elementArray, 0, largestPartId, null);
        Arrays.fill(elementWrittenArray, 0, largestPartId, false);
        largestPartId = -1;
    }

    /**
     * @return the largest part id added since the last clear, or -1 if nothing has been added
     */
    public int getLargestPartId() {
        return largestPartId;
    }

    public boolean isEmpty() {
        return largestPartId == -1;
    }

    /**
     * Writes the contents of this list to a buffer, with null entries represented
     * as a stream of 0s, and unwritten elements represented as the base entry.
     *
     * @param buffer the buffer to write to
     * @return the part index to be given to the struct of the model
     */
    public long writeToBuffer(SectionedPersistentBuffer buffer, MatrixStack.Entry baseMatrixEntry) {
        int matrixCount = getLargestPartId() + 1;
        long positionOffset = buffer.getPositionOffset().getAndAdd(matrixCount * BakedModelUtils.PART_STRUCT_SIZE);
        long pointer = buffer.getSectionedPointer() + positionOffset;

        for (int idx = 0; idx < elementArray.length; idx++) {
            if (elementWrittenArray[idx]) {
                MatrixStack.Entry matrixEntry = elementArray[idx];
                if (matrixEntry != null) {
                    writeMatrixEntry(pointer + idx * BakedModelUtils.PART_STRUCT_SIZE, matrixEntry);
                } else {
                    writeNullEntry(pointer + idx * BakedModelUtils.PART_STRUCT_SIZE);
                }
            } else {
                writeMatrixEntry(pointer + idx * BakedModelUtils.PART_STRUCT_SIZE, baseMatrixEntry);
            }
        }

        return positionOffset / BakedModelUtils.PART_STRUCT_SIZE;
    }

    private static void writeMatrixEntry(long pointer, MatrixStack.Entry matrixEntry) {
        Matrix4f model = matrixEntry.getModel();
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

        Matrix3f normal = matrixEntry.getNormal();
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
        MemoryUtil.memSet(pointer, 0, BakedModelUtils.PART_STRUCT_SIZE);
    }
}
