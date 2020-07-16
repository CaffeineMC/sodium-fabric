package me.jellysquid.mods.sodium.mixin.features.buffer_builder.fast_sort;

import com.google.common.primitives.Floats;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.VertexFormat;
import org.lwjgl.system.MemoryStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.Arrays;
import java.util.BitSet;

@Mixin(BufferBuilder.class)
public class MixinBufferBuilder {
    @Shadow
    private ByteBuffer buffer;

    @Shadow
    private int vertexCount;

    @Shadow
    private VertexFormat format;

    @Shadow
    private int buildStart;

    /**
     * @reason Reduce allocations, use stack allocations, avoid unnecessary math and pointer bumping, inline comparators
     * @author JellySquid
     */
    @Overwrite
    public void sortQuads(float cameraX, float cameraY, float cameraZ) {
        this.buffer.clear();
        FloatBuffer floatBuffer = this.buffer.asFloatBuffer();

        int vertexStride = this.format.getVertexSize();
        int quadStride = this.format.getVertexSizeInteger() * 4;

        int quadStart = this.buildStart / 4;
        int quadCount = this.vertexCount / 4;
        int vertexSizeInteger = this.format.getVertexSizeInteger();

        float[] distanceArray = new float[quadCount];
        int[] indicesArray = new int[quadCount];

        for (int quadIdx = 0; quadIdx < quadCount; ++quadIdx) {
            distanceArray[quadIdx] = getDistanceSq(floatBuffer, cameraX, cameraY, cameraZ, vertexSizeInteger, quadStart + (quadIdx * vertexStride));
            indicesArray[quadIdx] = quadIdx;
        }

        mergeSort(indicesArray, distanceArray);

        BitSet bits = new BitSet();

        try (MemoryStack stack = MemoryStack.stackPush()) {
            FloatBuffer tmp = stack.mallocFloat(vertexSizeInteger * 4);

            for (int l = bits.nextClearBit(0); l < indicesArray.length; l = bits.nextClearBit(l + 1)) {
                int m = indicesArray[l];

                if (m != l) {
                    sliceQuad(floatBuffer, m, quadStride, quadStart);
                    tmp.clear();
                    tmp.put(floatBuffer);

                    int n = m;

                    for (int o = indicesArray[m]; n != l; o = indicesArray[o]) {
                        sliceQuad(floatBuffer, o, quadStride, quadStart);
                        FloatBuffer floatBuffer3 = floatBuffer.slice();

                        sliceQuad(floatBuffer, n, quadStride, quadStart);
                        floatBuffer.put(floatBuffer3);

                        bits.set(n);
                        n = o;
                    }

                    sliceQuad(floatBuffer, l, quadStride, quadStart);
                    tmp.flip();

                    floatBuffer.put(tmp);
                }

                bits.set(l);
            }
        }
    }

    private static void mergeSort(int[] indicesArray, float[] distanceArray) {
        mergeSort(indicesArray, 0, indicesArray.length, distanceArray, Arrays.copyOf(indicesArray, indicesArray.length));
    }

    private static void sliceQuad(FloatBuffer floatBuffer, int quadIdx, int quadStride, int quadStart) {
        int base = quadStart + (quadIdx * quadStride);

        floatBuffer.limit(base + quadStride);
        floatBuffer.position(base);
    }

    private static float getDistanceSq(FloatBuffer buffer, float xCenter, float yCenter, float zCenter, int stride, int start) {
        int vertexBase = start;
        float x1 = buffer.get(vertexBase);
        float y1 = buffer.get(vertexBase + 1);
        float z1 = buffer.get(vertexBase + 2);

        vertexBase += stride;
        float x2 = buffer.get(vertexBase);
        float y2 = buffer.get(vertexBase + 1);
        float z2 = buffer.get(vertexBase + 2);

        vertexBase += stride;
        float x3 = buffer.get(vertexBase);
        float y3 = buffer.get(vertexBase + 1);
        float z3 = buffer.get(vertexBase + 2);

        vertexBase += stride;
        float x4 = buffer.get(vertexBase);
        float y4 = buffer.get(vertexBase + 1);
        float z4 = buffer.get(vertexBase + 2);

        float xDist = ((x1 + x2 + x3 + x4) * 0.25F) - xCenter;
        float yDist = ((y1 + y2 + y3 + y4) * 0.25F) - yCenter;
        float zDist = ((z1 + z2 + z3 + z4) * 0.25F) - zCenter;

        return (xDist * xDist) + (yDist * yDist) + (zDist * zDist);
    }

    private static void mergeSort(final int[] a, final int from, final int to, float[] dist, final int[] supp) {
        int len = to - from;

        // Insertion sort on smallest arrays
        if (len < 16) {
            insertionSort(a, from, to, dist);
            return;
        }

        // Recursively sort halves of a into supp
        final int mid = (from + to) >>> 1;
        mergeSort(supp, from, mid, dist, a);
        mergeSort(supp, mid, to, dist, a);

        // If list is already sorted, just copy from supp to a. This is an
        // optimization that results in faster sorts for nearly ordered lists.
        if (Floats.compare(dist[supp[mid]], dist[supp[mid - 1]]) <= 0) {
            System.arraycopy(supp, from, a, from, len);
            return;
        }

        // Merge sorted halves (now in supp) into a
        for (int i = from, p = from, q = mid; i < to; i++) {
            if (q >= to || p < mid && Floats.compare(dist[supp[q]], dist[supp[p]]) <= 0) {
                a[i] = supp[p++];
            } else {
                a[i] = supp[q++];
            }
        }
    }

    private static void insertionSort(final int[] a, final int from, final int to, final float[] dist) {
        for (int i = from; ++i < to; ) {
            int t = a[i];
            int j = i;

            for (int u = a[j - 1]; Floats.compare(dist[u], dist[t]) < 0; u = a[--j - 1]) {
                a[j] = u;
                if (from == j - 1) {
                    --j;
                    break;
                }
            }

            a[j] = t;
        }
    }

}
