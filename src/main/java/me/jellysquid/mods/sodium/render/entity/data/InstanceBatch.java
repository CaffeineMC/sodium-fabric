package me.jellysquid.mods.sodium.render.entity.data;

import it.unimi.dsi.fastutil.ints.IntArrays;

import java.util.List;

import me.jellysquid.mods.sodium.interop.vanilla.model.VboBackedModel;
import me.jellysquid.mods.sodium.render.entity.buffer.SectionedPersistentBuffer;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Matrix4f;
import org.lwjgl.system.MemoryUtil;

import java.util.ArrayList;

public class InstanceBatch {

    private final List<PerInstanceData> instances;
    private final MatrixEntryList matrices;

    private VboBackedModel model;
    private boolean indexed;
    private SectionedPersistentBuffer partBuffer;

    private VertexFormat.IntType indexType;
    private long indexStartingPos;
    private int indexCount;

    public InstanceBatch(VboBackedModel model, boolean indexed, int initialSize, SectionedPersistentBuffer partBuffer) {
        this.instances = new ArrayList<>(initialSize);
        this.matrices = new MatrixEntryList(initialSize);
        this.model = model;
        this.indexed = indexed;
        this.partBuffer = partBuffer;
    }

    public void reset(VboBackedModel model, boolean indexed, SectionedPersistentBuffer partBuffer) {
        this.model = model;
        this.indexed = indexed;
        this.partBuffer = partBuffer;

        instances.clear();
        matrices.clear();

        indexType = null;
        indexStartingPos = 0;
        indexCount = 0;
    }

    public boolean isIndexed() {
        return indexed;
    }

    public MatrixEntryList getMatrices() {
        return matrices;
    }

    public void writeInstancesToBuffer(SectionedPersistentBuffer buffer) {
        for (PerInstanceData perInstanceData : instances) {
            perInstanceData.writeToBuffer(buffer);
        }
    }

    public void addInstance(MatrixStack.Entry baseMatrixEntry, float red, float green, float blue, float alpha, int overlay, int light) {
        int overlayX = overlay & 0xFFFF;
        int overlayY = overlay >> 16 & 0xFFFF;
        int lightX = light & (LightmapTextureManager.field_32769 | 0xFF0F);
        int lightY = light >> 16 & (LightmapTextureManager.field_32769 | 0xFF0F);

        // this can happen if the model didn't render any modelparts,
        // in which case it makes sense to not try to render it anyway.
        if (matrices.isEmpty()) return;
        long partIndex = matrices.writeToBuffer(partBuffer, baseMatrixEntry);

        int[] primitiveIndices = null;
        int skippedPrimitivesStart = 0;
        if (indexed) {
            // Build the camera transforms from all the part transforms
            // This is how we quickly measure the depth of each primitive - find the
            // camera's position in model space, rather than applying the matrix multiply
            // to the primitive's position.
            int partIds = matrices.getLargestPartId() + 1;
            float[] cameraPositions = new float[partIds * 3];
            for (int partId = 0; partId < partIds; partId++) {
                Matrix4f mv;
                if (!matrices.getElementWritten(partId)) {
                    mv = baseMatrixEntry.getModel();
                } else {
                    MatrixStack.Entry entry = matrices.get(partId);
                    if (entry != null) {
                        mv = entry.getModel();
                    } else {
                        // skip empty part
                        continue;
                    }
                }

                // The translation of the inverse of a transform matrix is the negation of
                // the transposed rotation times the transform of the original matrix.
                //
                // The above only works if there's no scaling though - to correct for that, we
                // can find the length of each column is the scaling factor for x, y or z depending
                // on the column number. We then divide each of the output components by the
                // square of the scaling factor - since we're multiplying the scaling factor in a
                // second time with the matrix multiply, we have to divide twice (same as divide by sq root)
                // to get the actual inverse.

                // Using fastInverseSqrt might be playing with fire here
                double undoScaleX = 1.0 / Math.sqrt(mv.a00 * mv.a00 + mv.a10 * mv.a10 + mv.a20 * mv.a20);
                double undoScaleY = 1.0 / Math.sqrt(mv.a01 * mv.a01 + mv.a11 * mv.a11 + mv.a21 * mv.a21);
                double undoScaleZ = 1.0 / Math.sqrt(mv.a02 * mv.a02 + mv.a12 * mv.a12 + mv.a22 * mv.a22);

                int arrayIdx = partId * 3;
                cameraPositions[arrayIdx] = (float) (-(mv.a00 * mv.a03 + mv.a10 * mv.a13 + mv.a20 * mv.a23) * undoScaleX * undoScaleX);
                cameraPositions[arrayIdx + 1] = (float) (-(mv.a01 * mv.a03 + mv.a11 * mv.a13 + mv.a21 * mv.a23) * undoScaleY * undoScaleY);
                cameraPositions[arrayIdx + 2] = (float) (-(mv.a02 * mv.a03 + mv.a12 * mv.a13 + mv.a22 * mv.a23) * undoScaleZ * undoScaleZ);
            }

            float[] primitivePositions = model.getPrimitivePositions();
            int[] primitivePartIds = model.getPrimitivePartIds();
            int totalPrimitives = primitivePartIds.length;

            float[] primitiveSqDistances = new float[totalPrimitives];
            primitiveIndices = new int[totalPrimitives];
            for (int i = 0; i < totalPrimitives; i++) {
                primitiveIndices[i] = i;
            }

            for (int prim = 0; prim < totalPrimitives; prim++) {
                // skip if written as null
                int partId = primitivePartIds[prim];
                if (matrices.getElementWritten(partId) && matrices.get(partId) == null) {
                    primitiveSqDistances[prim] = Float.MIN_VALUE;
                    skippedPrimitivesStart++;
                }

                int primPosIdx = prim * 3;
                float x = primitivePositions[primPosIdx];
                float y = primitivePositions[primPosIdx + 1];
                float z = primitivePositions[primPosIdx + 2];

                int camPosIdx = partId * 3;
                float deltaX = x - cameraPositions[camPosIdx];
                float deltaY = y - cameraPositions[camPosIdx + 1];
                float deltaZ = z - cameraPositions[camPosIdx + 2];
                primitiveSqDistances[prim] = deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ;
            }

            // sort distances closest to furthest for front to back order
            IntArrays.quickSort(primitiveIndices, (i1, i2) -> Float.compare(primitiveSqDistances[i1], primitiveSqDistances[i2]));
        }

//        int skippedPrimitivesEnd = primitiveIndices == null ? 0 : (primitiveIndices.length - skippedPrimitivesStart) / 2;
        int skippedPrimitivesEnd = 0;
        instances.add(new PerInstanceData(partIndex, red, green, blue, alpha, overlayX, overlayY, lightX, lightY, primitiveIndices, skippedPrimitivesStart, skippedPrimitivesEnd));
    }

    public int size() {
        return instances.size();
    }

    public void writeIndicesToBuffer(VertexFormat.DrawMode drawMode, SectionedPersistentBuffer buffer) {
        if (!isIndexed()) return;

        // this is pretty slow
        indexCount = drawMode.getSize(drawMode.vertexCount * instances.stream().mapToInt(p -> p.primitiveIndices().length - p.skippedPrimitivesStart() - p.skippedPrimitivesEnd()).sum());
        indexType = VertexFormat.IntType.getSmallestTypeFor(indexCount);
        long sizeBytes = (long) indexCount * indexType.size;
        // add with alignment
        indexStartingPos = buffer.getPositionOffset().getAndAccumulate(sizeBytes, Long::sum);
        // sectioned pointer also has to be aligned
        long ptr = buffer.getSectionedPointer() + indexStartingPos;

        IndexWriter indexWriter = getIndexFunction(indexType, drawMode);
        int lastIndex = 0;
        for (PerInstanceData instanceData : instances) {
            int[] primitiveIndices = instanceData.primitiveIndices();
            int skippedPrimitivesStart = instanceData.skippedPrimitivesStart();
            int skippedPrimitivesEnd = instanceData.skippedPrimitivesEnd();
            for (int i = skippedPrimitivesStart; i < primitiveIndices.length - skippedPrimitivesEnd; i++) {
                int indexStart = lastIndex + primitiveIndices[i] * drawMode.vertexCount;
                indexWriter.writeIndices(ptr, indexStart, drawMode.vertexCount);
                ptr += (long) drawMode.getSize(drawMode.vertexCount) * indexType.size;
            }
            // we want to include the skipped primitives because the index needs to be calculated to the corresponding instance
            lastIndex += primitiveIndices.length * drawMode.vertexCount;
            // (primitiveIndices.length - skippedPrimitivesStart - skippedPrimitivesEnd) * drawMode.vertexCount;
        }
    }

    // The Cool Way(tm) to do index writing
    private static IndexWriter getIndexFunction(VertexFormat.IntType indexType, VertexFormat.DrawMode drawMode) {
        IndexWriter function;
        switch (indexType) {
            case BYTE -> {
                switch (drawMode) {
                    case LINES -> function = (ptr, startIdx, ignored) -> {
                        MemoryUtil.memPutByte(ptr, (byte) startIdx);
                        MemoryUtil.memPutByte(ptr + 1, (byte) (startIdx + 1));
                        MemoryUtil.memPutByte(ptr + 2, (byte) (startIdx + 2));
                        MemoryUtil.memPutByte(ptr + 3, (byte) (startIdx + 3));
                        MemoryUtil.memPutByte(ptr + 4, (byte) (startIdx + 2));
                        MemoryUtil.memPutByte(ptr + 5, (byte) (startIdx + 1));
                    };
                    case QUADS -> function = (ptr, startIdx, ignored) -> {
                        MemoryUtil.memPutByte(ptr, (byte) startIdx);
                        MemoryUtil.memPutByte(ptr + 1, (byte) (startIdx + 1));
                        MemoryUtil.memPutByte(ptr + 2, (byte) (startIdx + 2));
                        MemoryUtil.memPutByte(ptr + 3, (byte) (startIdx + 2));
                        MemoryUtil.memPutByte(ptr + 4, (byte) (startIdx + 3));
                        MemoryUtil.memPutByte(ptr + 5, (byte) startIdx);
                    };
                    default -> function = (ptr, startIdx, vertsPerPrim) -> {
                        for (int i = 0; i < drawMode.vertexCount; i++) {
                            MemoryUtil.memPutByte(ptr + i, (byte) (startIdx + i));
                        }
                    };
                }
            }
            case SHORT -> {
                switch (drawMode) {
                    case LINES -> function = (ptr, startIdx, ignored) -> {
                        MemoryUtil.memPutShort(ptr, (short) startIdx);
                        MemoryUtil.memPutShort(ptr + 2, (short) (startIdx + 1));
                        MemoryUtil.memPutShort(ptr + 4, (short) (startIdx + 2));
                        MemoryUtil.memPutShort(ptr + 6, (short) (startIdx + 3));
                        MemoryUtil.memPutShort(ptr + 8, (short) (startIdx + 2));
                        MemoryUtil.memPutShort(ptr + 10, (short) (startIdx + 1));
                    };
                    case QUADS -> function = (ptr, startIdx, ignored) -> {
                        MemoryUtil.memPutShort(ptr, (short) startIdx);
                        MemoryUtil.memPutShort(ptr + 2, (short) (startIdx + 1));
                        MemoryUtil.memPutShort(ptr + 4, (short) (startIdx + 2));
                        MemoryUtil.memPutShort(ptr + 6, (short) (startIdx + 2));
                        MemoryUtil.memPutShort(ptr + 8, (short) (startIdx + 3));
                        MemoryUtil.memPutShort(ptr + 10, (short) startIdx);
                    };
                    default -> function = (ptr, startIdx, vertsPerPrim) -> {
                        for (int i = 0; i < drawMode.vertexCount; i++) {
                            MemoryUtil.memPutShort(ptr + i * 2L, (short) (startIdx + i));
                        }
                    };
                }
            }
            case INT -> {
                switch (drawMode) {
                    case LINES -> function = (ptr, startIdx, ignored) -> {
                        MemoryUtil.memPutInt(ptr, startIdx);
                        MemoryUtil.memPutInt(ptr + 4, startIdx + 1);
                        MemoryUtil.memPutInt(ptr + 8, startIdx + 2);
                        MemoryUtil.memPutInt(ptr + 12, startIdx + 3);
                        MemoryUtil.memPutInt(ptr + 16, startIdx + 2);
                        MemoryUtil.memPutInt(ptr + 20, startIdx + 1);
                    };
                    case QUADS -> function = (ptr, startIdx, ignored) -> {
                        MemoryUtil.memPutInt(ptr, startIdx);
                        MemoryUtil.memPutInt(ptr + 4, startIdx + 1);
                        MemoryUtil.memPutInt(ptr + 8, startIdx + 2);
                        MemoryUtil.memPutInt(ptr + 12,startIdx + 2);
                        MemoryUtil.memPutInt(ptr + 16,startIdx + 3);
                        MemoryUtil.memPutInt(ptr + 20, startIdx);
                    };
                    default -> function = (ptr, startIdx, vertsPerPrim) -> {
                        for (int i = 0; i < drawMode.vertexCount; i++) {
                            MemoryUtil.memPutInt(ptr + i * 4L, startIdx + i);
                        }
                    };
                }
            }
            default -> throw new IllegalArgumentException("Index type " + indexType.name() + " unknown");
        }
        return function;
    }

    public long getIndexStartingPos() {
        return indexStartingPos;
    }

    public VertexFormat.IntType getIndexType() {
        return indexType;
    }

    public int getIndexCount() {
        return indexCount;
    }

}
