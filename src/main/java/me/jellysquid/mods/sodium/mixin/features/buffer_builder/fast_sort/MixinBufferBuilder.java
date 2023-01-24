
package me.jellysquid.mods.sodium.mixin.features.buffer_builder.fast_sort;

import me.jellysquid.mods.sodium.client.util.GeometrySort;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.VertexFormat;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;
import org.lwjgl.system.MemoryUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.nio.ByteBuffer;

@Mixin(BufferBuilder.class)
public abstract class MixinBufferBuilder {
    @Shadow
    private ByteBuffer buffer;

    @Shadow
    private VertexFormat.DrawMode drawMode;

    @Shadow
    private int elementOffset;

    @Shadow
    private float sortingCameraX;

    @Shadow
    private float sortingCameraY;

    @Shadow
    private float sortingCameraZ;

    @Shadow
    @Nullable
    private Vector3f[] sortingPrimitiveCenters;

    @Shadow
    private int vertexCount;

    @Shadow
    private VertexFormat format;

    /**
     * @author JellySquid
     * @reason Avoid slow memory accesses
     */
    @Overwrite
    private Vector3f[] buildPrimitiveCenters() {
        int primitiveCount = this.vertexCount / this.drawMode.additionalVertexCount;

        Vector3f[] centers = new Vector3f[primitiveCount];
        long start = MemoryUtil.memAddress(this.buffer, this.elementOffset);

        long vertexStride = this.format.getVertexSizeByte();
        long primitiveStride = vertexStride * this.drawMode.additionalVertexCount;

        for (int index = 0; index < primitiveCount; ++index) {
            long c1 = start + (index * primitiveStride);
            long c2 = c1 + (vertexStride * 2);

            float x1 = MemoryUtil.memGetFloat(c1);
            float y1 = MemoryUtil.memGetFloat(c1 + 4);
            float z1 = MemoryUtil.memGetFloat(c1 + 8);

            float x2 = MemoryUtil.memGetFloat(c2);
            float y2 = MemoryUtil.memGetFloat(c2 + 4);
            float z2 = MemoryUtil.memGetFloat(c2 + 8);

            centers[index] = new Vector3f((x1 + x2) * 0.5F, (y1 + y2) * 0.5F, (z1 + z2) * 0.5F);
        }

        return centers;
    }

    /**
     * @author JellySquid
     * @reason Use direct memory access, avoid indirection
     */
    @Overwrite
    private void writeSortedIndices(VertexFormat.IndexType indexType) {
        float[] distance = new float[this.sortingPrimitiveCenters.length];
        int[] indices = new int[this.sortingPrimitiveCenters.length];

        for(int i = 0; i < this.sortingPrimitiveCenters.length; ) {
            Vector3f pos = this.sortingPrimitiveCenters[i];

            float x = pos.x() - this.sortingCameraX;
            float y = pos.y() - this.sortingCameraY;
            float z = pos.z() - this.sortingCameraZ;

            distance[i] = (x * x) + (y * y) + (z * z);
            indices[i] = i++;
        }

        GeometrySort.mergeSort(indices, distance);

        long ptr = MemoryUtil.memAddress(this.buffer, this.elementOffset);
        int verticesPerPrimitive = this.drawMode.additionalVertexCount;

        switch (indexType) {
            case BYTE -> {
                for (int index : indices) {
                    int start = index * verticesPerPrimitive;

                    MemoryUtil.memPutByte(ptr + 0, (byte) (start + 0));
                    MemoryUtil.memPutByte(ptr + 1, (byte) (start + 1));
                    MemoryUtil.memPutByte(ptr + 2, (byte) (start + 2));
                    MemoryUtil.memPutByte(ptr + 3, (byte) (start + 2));
                    MemoryUtil.memPutByte(ptr + 4, (byte) (start + 3));
                    MemoryUtil.memPutByte(ptr + 5, (byte) (start + 0));

                    ptr += 6;
                }
            }
            case SHORT -> {
                for (int index : indices) {
                    int start = index * verticesPerPrimitive;

                    MemoryUtil.memPutShort(ptr + 0, (short) (start + 0));
                    MemoryUtil.memPutShort(ptr + 2, (short) (start + 1));
                    MemoryUtil.memPutShort(ptr + 4, (short) (start + 2));
                    MemoryUtil.memPutShort(ptr + 6, (short) (start + 2));
                    MemoryUtil.memPutShort(ptr + 8, (short) (start + 3));
                    MemoryUtil.memPutShort(ptr + 10, (short) (start + 0));

                    ptr += 12;
                }
            }
            case INT -> {
                for (int index : indices) {
                    int start = index * verticesPerPrimitive;

                    MemoryUtil.memPutInt(ptr + 0, start + 0);
                    MemoryUtil.memPutInt(ptr + 4, start + 1);
                    MemoryUtil.memPutInt(ptr + 8, start + 2);
                    MemoryUtil.memPutInt(ptr + 12, start + 2);
                    MemoryUtil.memPutInt(ptr + 16, start + 3);
                    MemoryUtil.memPutInt(ptr + 20, start + 0);

                    ptr += 24;
                }
            }
        }
    }
}