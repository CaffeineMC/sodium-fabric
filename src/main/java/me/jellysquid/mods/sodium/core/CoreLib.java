package me.jellysquid.mods.sodium.core;

import me.jellysquid.mods.sodium.client.render.chunk.graph.LocalSectionIndex;
import me.jellysquid.mods.sodium.client.render.chunk.lists.ChunkRenderList;
import me.jellysquid.mods.sodium.core.callback.PanicCallback;
import me.jellysquid.mods.sodium.core.types.CRegionDrawBatch;
import me.jellysquid.mods.sodium.core.types.CVec;
import org.joml.FrustumIntersection;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.*;

import java.nio.FloatBuffer;

import static org.lwjgl.system.MemoryUtil.*;

public class CoreLib {
    private static final PanicCallback CALLBACK = PanicCallback.defaultHandler();

    public static Frustum createFrustum(FrustumIntersection frustum, Vector3f offset) {
        long pFrustum;

        try (MemoryStack stack = MemoryStack.stackPush()) {
            var ppFrustum = stack.mallocPointer(1);

            var ppPoints = stack.mallocFloat(4 * 6);
            copyFrustumPoints(ppPoints, frustum);

            var pOffset = stack.mallocFloat(3);
            offset.get(pOffset);

            CoreLibFFI.frustumCreate(memAddress(ppFrustum), memAddress(ppPoints), memAddress(pOffset));
            pFrustum = ppFrustum.get(0);
        }

        return new Frustum(pFrustum);
    }

    public static void deleteFrustum(Frustum frustum) {
        CoreLibFFI.frustumDelete(frustum.ptr());
        frustum.invalidate();;
    }

    private static void copyFrustumPoints(FloatBuffer buf, FrustumIntersection frustum) {
        try {
            var field = FrustumIntersection.class.getDeclaredField("planes");
            field.setAccessible(true);

            var planes = (Vector4f[]) field.get(frustum);

            for (int i = 0; i < 6; i++) {
                buf.put((i * 4) + 0, planes[i].x);
                buf.put((i * 4) + 1, planes[i].y);
                buf.put((i * 4) + 2, planes[i].z);
                buf.put((i * 4) + 3, planes[i].w);
            }
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

    public static void graphAddChunk(Graph graph, int x, int y, int z) {
        CoreLibFFI.graphAddChunk(graph.ptr(), x, y, z);
    }

    public static void graphUpdateChunk(Graph graph, int x, int y, int z, GraphNode node) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            var pNode = stack.mallocInt(16);
            memPutLong(memAddress(pNode) + 0, node.connections);
            memPutInt(memAddress(pNode) + 8, node.flags);

            CoreLibFFI.graphUpdateChunk(graph.ptr(), x, y, z, memAddress(pNode));
        }
    }

    public static void graphRemoveChunk(Graph graph, int x, int y, int z) {
        CoreLibFFI.graphRemoveChunk(graph.ptr(), x, y, z);
    }

    public static ChunkRenderList graphSearch(Graph graph, Frustum frustum, int viewDistance) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            var drawBatches = CVec.stackAlloc(stack);
            CoreLibFFI.graphSearch(graph.ptr(), frustum.ptr(), viewDistance,
                    drawBatches.address());

            return new ChunkRenderList(drawBatches);
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
            PointerBuffer pfn = stack.mallocPointer(2);
            pfn.put(0 /* aligned_alloc */, allocator.getAlignedAlloc());
            pfn.put(1 /* aligned_free */, allocator.getAlignedFree());

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
