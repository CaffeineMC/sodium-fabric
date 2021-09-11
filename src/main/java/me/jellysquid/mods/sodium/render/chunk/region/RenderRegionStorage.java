package me.jellysquid.mods.sodium.render.chunk.region;

import me.jellysquid.mods.sodium.SodiumClient;
import me.jellysquid.mods.sodium.render.chunk.arena.AsyncBufferArena;
import me.jellysquid.mods.sodium.render.chunk.arena.GlBufferArena;
import me.jellysquid.mods.sodium.render.chunk.arena.SwapBufferArena;
import me.jellysquid.mods.sodium.render.chunk.arena.staging.StagingBuffer;
import me.jellysquid.mods.sodium.render.chunk.format.ModelVertexType;
import me.jellysquid.mods.thingl.device.RenderDevice;
import me.jellysquid.mods.thingl.tessellation.Tessellation;

import java.util.List;

public class RenderRegionStorage {
    private static final int EXPECTED_VERTICES_PER_CHUNK = 192;

    public final GlBufferArena vertexArena;
    public final GlBufferArena indicesArena;

    public Tessellation tessellation;

    private final RenderDevice device;

    public RenderRegionStorage(RenderDevice device) {
        int expectedVertexCount = RenderRegion.REGION_SIZE * EXPECTED_VERTICES_PER_CHUNK;
        int expectedIndexCount = (expectedVertexCount / 4) * 6;

        this.device = device;
        this.vertexArena = createArena(device, expectedVertexCount * ModelVertexType.INSTANCE.getBufferVertexFormat().getStride());
        this.indicesArena = createArena(device, expectedIndexCount * 4);
    }

    public void delete() {
        this.deleteTessellation();

        this.vertexArena.delete();
        this.indicesArena.delete();
    }

    public boolean isEmpty() {
        return this.vertexArena.isEmpty() && this.indicesArena.isEmpty();
    }

    public long getDeviceUsedMemory() {
        return this.vertexArena.getDeviceUsedMemory() + this.indicesArena.getDeviceUsedMemory();
    }

    public long getDeviceAllocatedMemory() {
        return this.vertexArena.getDeviceAllocatedMemory() + this.indicesArena.getDeviceAllocatedMemory();
    }

    private static GlBufferArena createArena(RenderDevice device, int initialCapacity) {
        return switch (SodiumClient.options().advanced.arenaMemoryAllocator) {
            case ASYNC -> new AsyncBufferArena(device, initialCapacity);
            case SWAP -> new SwapBufferArena(device);
        };
    }

    public void uploadAll(StagingBuffer stagingBuffer, List<RenderRegionManager.RegionUploadTask> queue) {
        boolean bufferChanged = this.vertexArena.upload(stagingBuffer, queue.stream().map(RenderRegionManager.RegionUploadTask::vertexData));
        bufferChanged |= this.indicesArena.upload(stagingBuffer, queue.stream().map(RenderRegionManager.RegionUploadTask::indexData));

        // If any of the buffers changed, the tessellation will need to be updated
        // Once invalidated the tessellation will be re-created on the next attempted use
        if (bufferChanged) {
            this.deleteTessellation();
        }
    }

    private void deleteTessellation() {
        if (this.tessellation != null) {
            this.device.deleteTessellation(this.tessellation);
            this.tessellation = null;
        }
    }
}
