package net.caffeinemc.sodium.render.entity.data;

import it.unimi.dsi.fastutil.ints.IntArrays;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.nio.ByteBuffer;
import java.util.List;
import net.caffeinemc.gfx.api.types.ElementFormat;
import net.caffeinemc.gfx.util.buffer.SequenceBuilder;
import net.caffeinemc.gfx.util.buffer.StreamingBuffer;
import net.caffeinemc.sodium.interop.vanilla.math.matrix.Matrix4fExtended;
import net.caffeinemc.sodium.interop.vanilla.math.matrix.MatrixUtil;
import net.caffeinemc.sodium.interop.vanilla.sequence.Blaze3DSequences;
import net.caffeinemc.sodium.render.entity.compile.BuiltEntityModel;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.util.math.MatrixStack;
import org.lwjgl.system.MemoryUtil;

public class EntityInstanceBatch {

    private final List<PerInstanceData> instances;
    private final MatrixEntryList matrices;

    private BuiltEntityModel model;
    private boolean indexed;
    private StreamingBuffer partBuffer;

    private ElementFormat elementFormat;
    private long indexStartingPos;
    private int indexCount;

    public EntityInstanceBatch(BuiltEntityModel model, boolean indexed, int initialSize, StreamingBuffer partBuffer) {
        this.instances = new ObjectArrayList<>(initialSize);
        this.matrices = new MatrixEntryList(initialSize);
        this.model = model;
        this.indexed = indexed;
        this.partBuffer = partBuffer;
    }

    public void reset(BuiltEntityModel model, boolean indexed, StreamingBuffer partBuffer) {
        this.model = model;
        this.indexed = indexed;
        this.partBuffer = partBuffer;

        this.instances.clear();
        this.matrices.clear();

        this.elementFormat = null;
        this.indexStartingPos = 0;
        this.indexCount = 0;
    }

    public boolean isIndexed() {
        return this.indexed;
    }

    public MatrixEntryList getMatrices() {
        return this.matrices;
    }

    public void writeInstancesToBuffer(StreamingBuffer buffer, int frameIndex) {
        ByteBuffer sectionView = buffer.getSection(frameIndex).getView();
        long pointer = MemoryUtil.memAddress(sectionView);
        int offset = 0;
        for (PerInstanceData perInstanceData : this.instances) {
            perInstanceData.writeToBuffer(pointer + offset);
            offset += PerInstanceData.STRUCT_SIZE_BYTES;
        }
        sectionView.position(sectionView.position() + offset);
    }

    public void addInstance(MatrixStack.Entry baseMatrixEntry, float red, float green, float blue, float alpha, int overlay, int light, int frameIndex) {
        int overlayX = overlay & 0xFFFF;
        int overlayY = overlay >> 16 & 0xFFFF;
        int lightX = light & (LightmapTextureManager.MAX_BLOCK_LIGHT_COORDINATE | 0xFF0F);
        int lightY = light >> 16 & (LightmapTextureManager.MAX_BLOCK_LIGHT_COORDINATE | 0xFF0F);

        // this can happen if the model didn't render any model parts,
        // in which case it makes sense to not try to render it anyway.
        if (this.matrices.isEmpty()) return;

        int[] primitiveIndices = null;
        int skippedPrimitivesStart = 0;
        if (this.indexed) {
            // Build the camera transforms from all the part transforms
            // This is how we quickly measure the depth of each primitive - find the
            // camera's position in model space, rather than applying the matrix multiply
            // to the primitive's position.
            int partIds = this.matrices.getLargestPartId() + 1;
            float[] cameraPositions = new float[partIds * 3];
            for (int partId = 0; partId < partIds; partId++) {
                Matrix4fExtended mv;
                if (!this.matrices.getElementWritten(partId)) {
                    mv = MatrixUtil.getExtendedMatrix(baseMatrixEntry.getPositionMatrix());
                } else {
                    MatrixStack.Entry entry = this.matrices.get(partId);
                    if (entry != null) {
                        mv = MatrixUtil.getExtendedMatrix(entry.getPositionMatrix());
                    } else {
                        // skip empty part
                        continue;
                    }
                }

                // TODO: update with mathutil version
                
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
                double undoScaleX = 1.0 / Math.sqrt(mv.getA00() * mv.getA00() + mv.getA10() * mv.getA10() + mv.getA20() * mv.getA20());
                double undoScaleY = 1.0 / Math.sqrt(mv.getA01() * mv.getA01() + mv.getA11() * mv.getA11() + mv.getA21() * mv.getA21());
                double undoScaleZ = 1.0 / Math.sqrt(mv.getA02() * mv.getA02() + mv.getA12() * mv.getA12() + mv.getA22() * mv.getA22());

                int arrayIdx = partId * 3;
                cameraPositions[arrayIdx] = (float) (-(mv.getA00() * mv.getA03() + mv.getA10() * mv.getA13() + mv.getA20() * mv.getA23()) * undoScaleX * undoScaleX);
                cameraPositions[arrayIdx + 1] = (float) (-(mv.getA01() * mv.getA03() + mv.getA11() * mv.getA13() + mv.getA21() * mv.getA23()) * undoScaleY * undoScaleY);
                cameraPositions[arrayIdx + 2] = (float) (-(mv.getA02() * mv.getA03() + mv.getA12() * mv.getA13() + mv.getA22() * mv.getA23()) * undoScaleZ * undoScaleZ);
            }

            float[] primitivePositions = this.model.primitivePositions();
            int[] primitivePartIds = this.model.primitivePartIds();
            int totalPrimitives = primitivePartIds.length;

            float[] primitiveSqDistances = new float[totalPrimitives];
            primitiveIndices = new int[totalPrimitives];
            for (int i = 0; i < totalPrimitives; i++) {
                primitiveIndices[i] = i;
            }

            for (int prim = 0; prim < totalPrimitives; prim++) {
                // skip if written as null
                int partId = primitivePartIds[prim];
                if (this.matrices.getElementWritten(partId) && this.matrices.get(partId) == null) {
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

        var section = this.partBuffer.getSection(frameIndex);
        int partIndex = section.getView().position() / MatrixEntryList.STRUCT_SIZE_BYTES;
        this.matrices.writeToBuffer(section.getView(), baseMatrixEntry);

        this.instances.add(new PerInstanceData(partIndex, red, green, blue, alpha, overlayX, overlayY, lightX, lightY, primitiveIndices, skippedPrimitivesStart, skippedPrimitivesEnd));
    }

    public int size() {
        return this.instances.size();
    }

    public void writeElements(VertexFormat.DrawMode drawMode, StreamingBuffer elementBuffer, int frameIndex) {
        if (!this.isIndexed()) return;

        if (drawMode.shareVertices) {
            throw new IllegalArgumentException("Draw mode cannot share vertices");
        }

        // this is pretty slow
        this.indexCount = drawMode.getIndexCount(drawMode.additionalVertexCount * this.instances.stream().mapToInt(p -> p.primitiveIndices().length - p.skippedPrimitivesStart() - p.skippedPrimitivesEnd()).sum());
        this.elementFormat = ElementFormat.getSmallestType(this.indexCount);
        int sizeBytes = this.indexCount * this.elementFormat.getSize();

        var section = elementBuffer.getSection(frameIndex);

        ByteBuffer sectionView = section.getView();
        int sectionPos = sectionView.position();
        this.indexStartingPos = sectionPos;
        // add with alignment
        sectionView.position(sectionPos + sizeBytes);
        // sectioned pointer also has to be aligned
        long pointer = MemoryUtil.memAddress0(section.getView()) + sectionPos;
        long pointerIncrement = (long) drawMode.getIndexCount(drawMode.additionalVertexCount) * this.elementFormat.getSize();

        SequenceBuilder sequenceBuilder = Blaze3DSequences.map(drawMode, this.elementFormat);
        int lastIndex = 0;
        for (PerInstanceData instanceData : this.instances) {
            int[] primitiveIndices = instanceData.primitiveIndices();
            int skippedPrimitivesStart = instanceData.skippedPrimitivesStart();
            int skippedPrimitivesEnd = instanceData.skippedPrimitivesEnd();
            for (int i = skippedPrimitivesStart; i < primitiveIndices.length - skippedPrimitivesEnd; i++) {
                int baseVertex = lastIndex + primitiveIndices[i] * drawMode.additionalVertexCount;
                sequenceBuilder.write(pointer, baseVertex);
                pointer += pointerIncrement;
            }
            // we want to include the skipped primitives because the index needs to be calculated to the corresponding instance
            lastIndex += primitiveIndices.length * drawMode.additionalVertexCount;
            // (primitiveIndices.length - skippedPrimitivesStart - skippedPrimitivesEnd) * drawMode.vertexCount;
        }
    }

    public long getIndexStartingPos() {
        return this.indexStartingPos;
    }

    public ElementFormat getElementFormat() {
        return this.elementFormat;
    }

    public int getIndexCount() {
        return this.indexCount;
    }

    public record PerInstanceData(long partArrayIndex, float red, float green, float blue, float alpha, int overlayX,
                                  int overlayY, int lightX, int lightY, int[] primitiveIndices, int skippedPrimitivesStart,
                                  int skippedPrimitivesEnd) {

        public static final long STRUCT_SIZE_BYTES = 4 * Float.BYTES + 2 * Integer.BYTES + 2 * Integer.BYTES + 3 * Float.BYTES + Integer.BYTES;

        public void writeToBuffer(long pointer) {
            MemoryUtil.memPutFloat(pointer, this.red);
            MemoryUtil.memPutFloat(pointer + 4, this.green);
            MemoryUtil.memPutFloat(pointer + 8, this.blue);
            MemoryUtil.memPutFloat(pointer + 12, this.alpha);
            MemoryUtil.memPutInt(pointer + 16, this.overlayX);
            MemoryUtil.memPutInt(pointer + 20, this.overlayY);
            MemoryUtil.memPutInt(pointer + 24, this.lightX);
            MemoryUtil.memPutInt(pointer + 28, this.lightY);
            // if this overflows, we have to change it to an u64 in the shader. also, figure out how to actually calculate this as an uint.
            MemoryUtil.memPutInt(pointer + 44, (int) this.partArrayIndex);
        }

    }

}
