package me.jellysquid.mods.sodium.render.chunk.region;

import it.unimi.dsi.fastutil.longs.Long2ReferenceOpenHashMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import me.jellysquid.mods.sodium.SodiumClient;
import me.jellysquid.mods.sodium.SodiumRender;
import me.jellysquid.mods.sodium.render.chunk.arena.PendingUpload;
import me.jellysquid.mods.sodium.render.chunk.arena.staging.FallbackStagingBuffer;
import me.jellysquid.mods.sodium.render.chunk.arena.staging.MappedStagingBuffer;
import me.jellysquid.mods.sodium.render.chunk.arena.staging.StagingBuffer;
import me.jellysquid.mods.sodium.render.IndexedMesh;
import me.jellysquid.mods.thingl.device.RenderDevice;
import me.jellysquid.mods.sodium.render.chunk.renderer.ChunkGraphicsState;
import me.jellysquid.mods.sodium.render.chunk.RenderSection;
import me.jellysquid.mods.sodium.render.chunk.compile.ChunkBuildResult;
import me.jellysquid.mods.sodium.render.chunk.data.BuiltChunkMesh;
import me.jellysquid.mods.sodium.render.chunk.passes.BlockRenderPass;
import org.joml.FrustumIntersection;

import java.util.*;

public class RenderRegionManager {
    private final Long2ReferenceOpenHashMap<RenderRegion> regions = new Long2ReferenceOpenHashMap<>();

    private final StagingBuffer stagingBuffer;

    public RenderRegionManager(RenderDevice device) {
        this.stagingBuffer = createStagingBuffer(device);
    }

    public void updateVisibility(FrustumIntersection frustum) {
        for (RenderRegion region : this.regions.values()) {
            if (!region.isEmpty()) {
                region.updateVisibility(frustum);
            }
        }
    }

    public void cleanup() {
        this.stagingBuffer.flip();

        Iterator<RenderRegion> it = this.regions.values()
                .iterator();

        while (it.hasNext()) {
            RenderRegion region = it.next();
            region.deleteUnusedStorage();

            if (region.isEmpty()) {
                it.remove();
            }
        }
    }

    public void upload(Iterator<ChunkBuildResult> queue) {
        for (Map.Entry<RenderRegion, List<ChunkBuildResult>> entry : this.setupUploadBatches(queue).entrySet()) {
            RenderRegion region = entry.getKey();
            List<ChunkBuildResult> uploadQueue = entry.getValue();

            this.upload(region, uploadQueue);

            for (ChunkBuildResult result : uploadQueue) {
                result.render.onBuildFinished(result);
                result.delete();
            }
        }

        this.stagingBuffer.flush();
    }

    private void upload(RenderRegion region, List<ChunkBuildResult> results) {
        Map<BlockRenderPass, List<RegionUploadTask>> queues = new Reference2ObjectOpenHashMap<>();

        for (ChunkBuildResult result : results) {
            // De-allocate all storage for data we're about to replace
            // This will allow it to be cheaply re-allocated just below
            result.render.deleteGraphicsState();

            for (Map.Entry<BlockRenderPass, BuiltChunkMesh> entry : result.getMeshes()) {
                BlockRenderPass pass = entry.getKey();
                BuiltChunkMesh meshData = entry.getValue();

                if (meshData != null) {
                    IndexedMesh vertexData = meshData.getVertexData();

                    var queue = queues.computeIfAbsent(pass, k -> new ArrayList<>());
                    queue.add(new RegionUploadTask(result.render, meshData,
                            new PendingUpload(vertexData.vertexBuffer()),
                            new PendingUpload(vertexData.indexBuffer())));
                }
            }
        }

        // If we have nothing to upload, abort!
        if (queues.isEmpty()) {
            return;
        }

        for (Map.Entry<BlockRenderPass, List<RegionUploadTask>> entry : queues.entrySet()) {
            BlockRenderPass pass = entry.getKey();

            RenderRegionStorage storage = region.initStorage(pass);
            List<RegionUploadTask> queue = entry.getValue();

            storage.uploadAll(this.stagingBuffer, queue);

            // Collect the upload results
            for (RegionUploadTask upload : queue) {
                upload.section.setGraphicsState(pass, new ChunkGraphicsState(upload.vertexData.getResult(), upload.indexData.getResult(), upload.meshData));
            }
        }
    }

    private Map<RenderRegion, List<ChunkBuildResult>> setupUploadBatches(Iterator<ChunkBuildResult> renders) {
        Map<RenderRegion, List<ChunkBuildResult>> map = new Reference2ObjectLinkedOpenHashMap<>();

        while (renders.hasNext()) {
            ChunkBuildResult result = renders.next();
            RenderSection render = result.render;

            if (!render.canAcceptBuildResults(result)) {
                result.delete();

                continue;
            }

            RenderRegion region = this.regions.get(RenderRegion.getRegionKeyForChunk(render.getChunkX(), render.getChunkY(), render.getChunkZ()));

            if (region == null) {
                // Discard the result if the region is no longer loaded
                result.delete();

                continue;
            }

            List<ChunkBuildResult> uploadQueue = map.computeIfAbsent(region, k -> new ArrayList<>());
            uploadQueue.add(result);
        }

        return map;
    }

    public RenderRegion createRegionForChunk(int x, int y, int z) {
        long key = RenderRegion.getRegionKeyForChunk(x, y, z);
        RenderRegion region = this.regions.get(key);

        if (region == null) {
            this.regions.put(key, region = RenderRegion.createRegionForChunk(this, x, y, z));
        }

        return region;
    }

    public void delete() {
        for (RenderRegion region : this.regions.values()) {
            region.deleteResources();
        }

        this.regions.clear();
        this.stagingBuffer.delete();
    }

    public Collection<RenderRegion> getLoadedRegions() {
        return this.regions.values();
    }

    public StagingBuffer getStagingBuffer() {
        return this.stagingBuffer;
    }

    private static StagingBuffer createStagingBuffer(RenderDevice device) {
        if (SodiumClient.options().advanced.useAdvancedStagingBuffers && MappedStagingBuffer.isSupported(SodiumRender.DEVICE)) {
            return new MappedStagingBuffer(device);
        }

        return new FallbackStagingBuffer(device);
    }

    public record RegionUploadTask(RenderSection section, BuiltChunkMesh meshData, PendingUpload vertexData, PendingUpload indexData) {
    }
}
