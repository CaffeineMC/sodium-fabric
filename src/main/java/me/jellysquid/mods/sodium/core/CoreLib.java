package me.jellysquid.mods.sodium.core;

import me.jellysquid.mods.sodium.client.render.chunk.lists.SectionRenderList;
import me.jellysquid.mods.sodium.core.callback.PanicCallback;
import me.jellysquid.mods.sodium.core.types.CVec;
import org.joml.FrustumIntersection;
import org.joml.Vector3d;
import org.joml.Vector4f;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import java.nio.FloatBuffer;

import static org.lwjgl.system.MemoryUtil.*;

public class CoreLib {
    private static final PanicCallback CALLBACK = PanicCallback.defaultHandler();

    public static Frustum createFrustum(FrustumIntersection frustum, Vector3d offset) {
        long pFrustum;

        try (MemoryStack stack = MemoryStack.stackPush()) {
            var ppFrustum = stack.mallocPointer(1);

            var ppPoints = stack.mallocFloat(6 * 4);
            copyFrustumPoints(ppPoints, frustum);

            var pOffset = stack.mallocDouble(3);
            offset.get(pOffset);

            CoreLibFFI.frustumCreate(memAddress(ppFrustum), memAddress(ppPoints), memAddress(pOffset));
            pFrustum = ppFrustum.get(0);
        }

        return new Frustum(pFrustum);
    }

    public static void deleteFrustum(Frustum frustum) {
        CoreLibFFI.frustumDelete(frustum.ptr());
        frustum.invalidate();
    }

    private static void copyFrustumPoints(FloatBuffer buf, FrustumIntersection frustum) {
        try {
            var field = FrustumIntersection.class.getDeclaredField("planes");
            field.setAccessible(true);

            var planes = (Vector4f[]) field.get(frustum);

            buf.put(planes[0].x);
            buf.put(planes[1].x);
            buf.put(planes[2].x);
            buf.put(planes[3].x);
            buf.put(planes[4].x);
            buf.put(planes[5].x);

            buf.put(planes[0].y);
            buf.put(planes[1].y);
            buf.put(planes[2].y);
            buf.put(planes[3].y);
            buf.put(planes[4].y);
            buf.put(planes[5].y);

            buf.put(planes[0].z);
            buf.put(planes[1].z);
            buf.put(planes[2].z);
            buf.put(planes[3].z);
            buf.put(planes[4].z);
            buf.put(planes[5].z);

            buf.put(planes[0].w);
            buf.put(planes[1].w);
            buf.put(planes[2].w);
            buf.put(planes[3].w);
            buf.put(planes[4].w);
            buf.put(planes[5].w);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Failed to extract planes from frustum", e);
        }
    }

    public static Graph createGraph() {
        long pGraph;

        try (MemoryStack stack = MemoryStack.stackPush()) {
            var ppGraph = stack.mallocPointer(1);

            CoreLibFFI.graphCreate(ppGraph.address());
            pGraph = ppGraph.get(0);
        }

        return new Graph(pGraph);
    }

    public static void deleteGraph(Graph graph) {
        CoreLibFFI.graphDelete(graph.ptr());
        graph.invalidate();
    }

    public static void graphAddSection(Graph graph, int x, int y, int z) {
        CoreLibFFI.graphAddSection(graph.ptr(), x, y, z);
    }

    public static void graphUpdateSection(Graph graph, int x, int y, int z, long connections, int flags) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            var pNode = stack.mallocInt(16);
            memPutLong(memAddress(pNode) + 0, connections);
            memPutInt(memAddress(pNode) + 8, flags);

            CoreLibFFI.graphUpdateSection(graph.ptr(), x, y, z, memAddress(pNode));
        }
    }

    public static void graphRemoveSection(Graph graph, int x, int y, int z) {
        CoreLibFFI.graphRemoveSection(graph.ptr(), x, y, z);
    }

    public static SectionRenderList graphSearch(Graph graph, Frustum frustum, int viewDistance) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            var drawBatches = CVec.stackAlloc(stack);
            // TODO: deallocate the cvec at some point, because right now we never do...
            CoreLibFFI.graphSearch(graph.ptr(), frustum.ptr(), viewDistance,
                    drawBatches.address());

            return new SectionRenderList(drawBatches);
        }
    }

    public static void checkError(int result) {
        switch (result) {
            case 0 /* no error */ -> { }
            case 1 /* out of memory */ -> throw new OutOfMemoryError();
        }
    }

    public static void init() {
        CoreLib.initAllocator(MemoryUtil.getAllocator());
        CoreLib.initPanicHandler();
    }

    private static void initPanicHandler() {
        CoreLibFFI.setPanicHandler(CALLBACK.address());
    }

    private static void initAllocator(MemoryUtil.MemoryAllocator allocator) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            PointerBuffer pfn = stack.mallocPointer(4);
            pfn.put(0 /* aligned_alloc */, allocator.getAlignedAlloc());
            pfn.put(1 /* aligned_free */, allocator.getAlignedFree());
            pfn.put(2 /* realloc */, allocator.getRealloc());
            pfn.put(3 /* calloc */, allocator.getCalloc());

            CoreLibFFI.setAllocator(pfn.address());
        }
    }

    public static class Graph extends WrappedPointer {
        protected Graph(long handle) {
            super(handle);
        }
    }

    public static class Frustum extends WrappedPointer {
        protected Frustum(long handle) {
            super(handle);
        }
    }
}
