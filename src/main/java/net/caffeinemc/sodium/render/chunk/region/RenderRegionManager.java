package net.caffeinemc.sodium.render.chunk.region;

import it.unimi.dsi.fastutil.longs.Long2ReferenceOpenHashMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectLinkedOpenHashMap;
import net.caffeinemc.gfx.api.device.RenderDevice;
import net.caffeinemc.sodium.interop.vanilla.math.frustum.Frustum;
import net.caffeinemc.sodium.render.arena.BufferSegment;
import net.caffeinemc.sodium.render.arena.PendingUpload;
import net.caffeinemc.sodium.render.chunk.RenderSection;
import net.caffeinemc.sodium.render.chunk.compile.tasks.TerrainBuildResult;
import net.caffeinemc.sodium.render.chunk.state.BuiltChunkGeometry;
import net.caffeinemc.sodium.render.chunk.state.UploadedChunkGeometry;
import net.caffeinemc.sodium.render.terrain.format.TerrainVertexType;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

public class RenderRegionManager {
    private final Long2ReferenceOpenHashMap<RenderRegion> regions = new Long2ReferenceOpenHashMap<>();

    private final RenderDevice device;
    private final TerrainVertexType vertexType;

    public RenderRegionManager(RenderDevice device, TerrainVertexType vertexType) {
        this.device = device;
        this.vertexType = vertexType;
    }

    public void updateVisibility(Frustum frustum) {
        for (RenderRegion region : this.regions.values()) {
            if (!region.isEmpty()) {
                region.updateVisibility(frustum);
            }
        }
    }

    public void cleanup() {
        Iterator<RenderRegion> it = this.regions.values()
                .iterator();

        while (it.hasNext()) {
            RenderRegion region = it.next();

            if (region.isEmpty()) {
                region.deleteResources();
                it.remove();
            }
        }
    }

    public void upload(Iterator<TerrainBuildResult> queue) {
        for (Map.Entry<RenderRegion, List<TerrainBuildResult>> entry : this.setupUploadBatches(queue).entrySet()) {
            RenderRegion region = entry.getKey();
            List<TerrainBuildResult> uploadQueue = entry.getValue();

            this.upload(region, uploadQueue);

            for (TerrainBuildResult result : uploadQueue) {
                result.render().onBuildFinished(result);
                result.delete();
            }
        }
    }

    private void upload(RenderRegion region, List<TerrainBuildResult> results) {
        List<PendingUpload> uploads = new ArrayList<>();
        List<ChunkGeometryUpload> jobs = new ArrayList<>(results.size());

        for (TerrainBuildResult result : results) {
            var render = result.render();
            var geometry = result.geometry();

            // De-allocate all storage for the meshes we're about to replace
            // This will allow it to be cheaply re-allocated later
            render.deleteGeometry();

            // Only submit an upload job if there is data in the first place
            var vertices = geometry.vertices();

            if (vertices != null) {
                var upload = new PendingUpload(vertices.buffer());
                jobs.add(new ChunkGeometryUpload(render, geometry, upload.holder));

                uploads.add(upload);
            }
        }

        // If we have nothing to upload, don't allocate a region
        if (jobs.isEmpty()) {
            return;
        }

        RenderRegion.Resources resources = region.getOrCreateArenas();
        resources.vertexBuffers.upload(uploads);

        // Collect the upload results
        for (ChunkGeometryUpload upload : jobs) {
            upload.section.updateGeometry(new UploadedChunkGeometry(upload.result.get(), upload.geometry.models()));
        }
    }

    private Map<RenderRegion, List<TerrainBuildResult>> setupUploadBatches(Iterator<TerrainBuildResult> renders) {
        Map<RenderRegion, List<TerrainBuildResult>> map = new Reference2ObjectLinkedOpenHashMap<>();

        while (renders.hasNext()) {
            TerrainBuildResult result = renders.next();
            RenderSection render = result.render();

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

    public void delete() {
        for (RenderRegion region : this.regions.values()) {
            region.deleteResources();
        }

        this.regions.clear();
    }

    public Collection<RenderRegion> getLoadedRegions() {
        return this.regions.values();
    }

    protected RenderRegion.Resources createRegionArenas() {
        return new RenderRegion.Resources(this.device, this.vertexType);
    }

    private record ChunkGeometryUpload(RenderSection section, BuiltChunkGeometry geometry, AtomicReference<BufferSegment> result) {

    }
}
