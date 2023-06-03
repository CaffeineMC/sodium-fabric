package me.jellysquid.mods.sodium.client.render.chunk.data;

import me.jellysquid.mods.sodium.client.model.quad.properties.ModelQuadFacing;
import org.lwjgl.system.MemoryUtil;

// This code is a terrible hack to get around the fact that we are so incredibly memory bound, and that we
// have no control over memory layout. The chunk rendering code spends an astronomical amount of time chasing
// object pointers that are scattered across the heap. Worse yet, because render state is initialized over a long
// period of time as the world loads, those objects are never even remotely close to one another in heap, so
// you also have to pay the penalty of a DTLB miss on every other access.
//
// Unfortunately, Hotspot *still* produces abysmal machine code for the chunk rendering code paths, since any usage of
// unsafe memory intrinsics seems to cause it to become paranoid about memory aliasing. Well, that, and it just produces
// terrible machine code in pretty much every critical code path we seem to have...
//
// Please never try to write performance critical code in Java. This is what it will do to you. And you will still be
// three times slower than the most naive solution in literally any other language that LLVM can compile.
public class SectionRenderDataUnsafe {
    private static final long OFFSET_BASE_VERTEX = 0;

    private static final long OFFSET_MODEL_PARTS = 4;

    private static final long STRIDE = 4L + (4L * ModelQuadFacing.COUNT);

    public static long allocateHeap(int count) {
        return MemoryUtil.nmemAlloc(STRIDE * count);
    }

    public static void freeHeap(long pointer) {
        MemoryUtil.nmemFree(pointer);
    }

    public static void clear(long pointer) {
        MemoryUtil.memPutInt(pointer + OFFSET_BASE_VERTEX, -1);
        MemoryUtil.memSet(pointer + OFFSET_MODEL_PARTS, 0x0, ModelQuadFacing.COUNT * Integer.BYTES);
    }

    public static long heapPointer(long pArrayBase, int index) {
        return pArrayBase + (index * STRIDE);
    }

    public static int getBaseVertex(long pointer) {
        return MemoryUtil.memGetInt(pointer + OFFSET_BASE_VERTEX);
    }

    public static void setBaseVertex(long pointer, int value) {
        MemoryUtil.memPutInt(pointer + OFFSET_BASE_VERTEX, value);
    }

    public static void setBatchSize(long pArray, int index, int value) {
        MemoryUtil.memPutInt(pArray + (OFFSET_MODEL_PARTS + (index * 4L)), value);
    }

    public static int getBatchSize(long pArray, int index) {
        return MemoryUtil.memGetInt(pArray + (OFFSET_MODEL_PARTS + (index * 4L)));
    }

    public static boolean isEmpty(long pointer) {
        return MemoryUtil.memGetInt(pointer + OFFSET_BASE_VERTEX) == -1;
    }
}
