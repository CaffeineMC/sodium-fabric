package me.jellysquid.mods.sodium.render.chunk.region;

import it.unimi.dsi.fastutil.longs.Long2ReferenceOpenHashMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectLinkedOpenHashMap;
import me.jellysquid.mods.sodium.render.chunk.arena.PendingUpload;
import me.jellysquid.mods.sodium.render.chunk.buffer.IndexedVertexData;
import me.jellysquid.mods.sodium.opengl.device.CommandList;
import me.jellysquid.mods.sodium.opengl.device.RenderDevice;
import me.jellysquid.mods.sodium.render.stream.MappedStreamingBuffer;
import me.jellysquid.mods.sodium.render.stream.StreamingBuffer;
import me.jellysquid.mods.sodium.interop.vanilla.math.frustum.Frustum;
import me.jellysquid.mods.sodium.render.chunk.state.UploadedChunkMesh;
import me.jellysquid.mods.sodium.render.chunk.RenderSection;
import me.jellysquid.mods.sodium.render.chunk.compile.tasks.TerrainBuildResult;
import me.jellysquid.mods.sodium.render.chunk.state.ChunkMesh;
import me.jellysquid.mods.sodium.render.chunk.passes.ChunkRenderPass;

import java.util.*;

public class RenderRegionManager {
    private final Long2ReferenceOpenHashMap<RenderRegion> regions = new Long2ReferenceOpenHashMap<>();

    private final StreamingBuffer streamingBuffer;

    public RenderRegionManager(CommandList commandList) {
        this.streamingBuffer = createStagingBuffer(commandList);
    }

    public void updateVisibility(Frustum frustum) {
        for (RenderRegion region : this.regions.values()) {
            if (!region.isEmpty()) {
                region.updateVisibility(frustum);
            }
        }
    }

    public void cleanup() {
        try (CommandList commandList = RenderDevice.INSTANCE.createCommandList()) {
            this.streamingBuffer.flush(commandList);

            Iterator<RenderRegion> it = this.regions.values()
                    .iterator();

            while (it.hasNext()) {
                RenderRegion region = it.next();

                if (region.isEmpty()) {
                    region.deleteResources(commandList);

                    it.remove();
                }
            }
        }
    }

    public void upload(CommandList commandList, Iterator<TerrainBuildResult> queue) {
        for (Map.Entry<RenderRegion, List<TerrainBuildResult>> entry : this.setupUploadBatches(queue).entrySet()) {
            RenderRegion region = entry.getKey();
            List<TerrainBuildResult> uploadQueue = entry.getValue();

            this.upload(commandList, region, uploadQueue);

            for (TerrainBuildResult result : uploadQueue) {
                result.render.onBuildFinished(result);

                result.delete();
            }
        }
    }

    private void upload(CommandList commandList, RenderRegion region, List<TerrainBuildResult> results) {
        List<PendingSectionUpload> sectionUploads = new ArrayList<>();

        for (TerrainBuildResult result : results) {
            for (ChunkRenderPass pass : ChunkRenderPass.VALUES) {
                UploadedChunkMesh graphics = result.render.updateMesh(pass, null);

                // De-allocate all storage for data we're about to replace
                // This will allow it to be cheaply re-allocated just below
                if (graphics != null) {
                    graphics.delete();
                }

                ChunkMesh meshData = result.getMesh(pass);

                if (meshData != null) {
                    IndexedVertexData vertexData = meshData.getVertexData();

                    sectionUploads.add(new PendingSectionUpload(result.render, meshData, pass,
                            new PendingUpload(vertexData.vertexBuffer()),
                            new PendingUpload(vertexData.indexBuffer())));
                }
            }
        }

        // If we have nothing to upload, abort!
        if (sectionUploads.isEmpty()) {
            return;
        }

        RenderRegion.RenderRegionArenas arenas = region.getOrCreateArenas(commandList);

        arenas.vertexBuffers.upload(commandList, sectionUploads.stream().map(i -> i.vertexUpload));
        arenas.indexBuffers.upload(commandList, sectionUploads.stream().map(i -> i.indicesUpload));

        // Collect the upload results
        for (PendingSectionUpload upload : sectionUploads) {
            upload.section.updateMesh(upload.pass, new UploadedChunkMesh(upload.vertexUpload.getResult(), upload.indicesUpload.getResult(), upload.meshData));
        }
    }

    private Map<RenderRegion, List<TerrainBuildResult>> setupUploadBatches(Iterator<TerrainBuildResult> renders) {
        Map<RenderRegion, List<TerrainBuildResult>> map = new Reference2ObjectLinkedOpenHashMap<>();

        while (renders.hasNext()) {
            TerrainBuildResult result = renders.next();
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

            List<TerrainBuildResult> uploadQueue = map.computeIfAbsent(region, k -> new ArrayList<>());
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

    public void delete(CommandList commandList) {
        for (RenderRegion region : this.regions.values()) {
            region.deleteResources(commandList);
        }

        this.regions.clear();
        this.streamingBuffer.delete(commandList);
    }

    public Collection<RenderRegion> getLoadedRegions() {
        return this.regions.values();
    }

    public StreamingBuffer getStreamingBuffer() {
        return this.streamingBuffer;
    }

    protected RenderRegion.RenderRegionArenas createRegionArenas(CommandList commandList) {
        return new RenderRegion.RenderRegionArenas(commandList, this.streamingBuffer);
    }

    private static StreamingBuffer createStagingBuffer(CommandList commandList) {
        return new MappedStreamingBuffer(commandList, 16 * 1024 * 1024);
    }

    private record PendingSectionUpload(RenderSection section, ChunkMesh meshData, ChunkRenderPass pass,
                                        PendingUpload vertexUpload, PendingUpload indicesUpload) {
    }
}
