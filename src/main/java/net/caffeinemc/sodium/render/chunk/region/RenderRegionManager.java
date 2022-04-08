package net.caffeinemc.sodium.render.chunk.region;

import it.unimi.dsi.fastutil.longs.Long2ReferenceMap;
import it.unimi.dsi.fastutil.longs.Long2ReferenceOpenHashMap;
import it.unimi.dsi.fastutil.objects.ReferenceArrayList;
import net.caffeinemc.gfx.api.device.RenderDevice;
import net.caffeinemc.sodium.render.arena.BufferSegment;
import net.caffeinemc.sodium.render.arena.PendingUpload;
import net.caffeinemc.sodium.render.chunk.RenderSection;
import net.caffeinemc.sodium.render.chunk.compile.tasks.TerrainBuildResult;
import net.caffeinemc.sodium.render.chunk.state.BuiltChunkGeometry;
import net.caffeinemc.sodium.render.chunk.state.ChunkRenderData;
import net.caffeinemc.sodium.render.chunk.state.UploadedChunkGeometry;
import net.caffeinemc.sodium.render.terrain.format.TerrainVertexType;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;

public class RenderRegionManager {
    private final Long2ReferenceOpenHashMap<RenderRegion> regions = new Long2ReferenceOpenHashMap<>();

    private final RenderDevice device;
    private final TerrainVertexType vertexType;

    public RenderRegionManager(RenderDevice device, TerrainVertexType vertexType) {
        this.device = device;
        this.vertexType = vertexType;
    }

    public RenderRegion getRegion(long regionId) {
        return this.regions.get(regionId);
    }

    public void cleanup() {
        Iterator<RenderRegion> it = this.regions.values()
                .iterator();

        while (it.hasNext()) {
            RenderRegion region = it.next();

            if (region.isEmpty()) {
                region.delete();
                it.remove();
            }
        }
    }

    public void uploadChunks(Iterator<TerrainBuildResult> queue, @Deprecated RenderUpdateCallback callback) {
        for (var entry : this.setupUploadBatches(queue)) {
            this.uploadGeometryBatch(entry.getLongKey(), entry.getValue());

            for (TerrainBuildResult result : entry.getValue()) {
                RenderSection section = result.render();

                if (section.data() != null) {
                    callback.accept(section, section.data(), result.data());
                }

                section.setData(result.data());
                section.setLastAcceptedBuildTime(result.buildTime());

                result.delete();
            }
        }
    }

    public interface RenderUpdateCallback {
        void accept(RenderSection section, ChunkRenderData prev, ChunkRenderData next);
    }

    private void uploadGeometryBatch(long regionKey, List<TerrainBuildResult> results) {
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

        RenderRegion region = this.regions.get(regionKey);

        if (region == null) {
            this.regions.put(regionKey, region = new RenderRegion(this.device, this.vertexType));
        }

        region.vertexBuffers.upload(uploads);

        // Collect the upload results
        for (ChunkGeometryUpload upload : jobs) {
            upload.section.updateGeometry(new UploadedChunkGeometry(upload.result.get(), upload.geometry.models()));
        }
    }

    private Iterable<Long2ReferenceMap.Entry<List<TerrainBuildResult>>> setupUploadBatches(Iterator<TerrainBuildResult> renders) {
        var batches = new Long2ReferenceOpenHashMap<List<TerrainBuildResult>>();

        while (renders.hasNext()) {
            TerrainBuildResult result = renders.next();
            RenderSection render = result.render();

            if (render.isDisposed() || result.buildTime() <= render.getLastAcceptedBuildTime()) {
                result.delete();

                continue;
            }

            var batch = batches.computeIfAbsent(render.getRegionKey(), key -> new ReferenceArrayList<>());
            batch.add(result);
        }

        return batches.long2ReferenceEntrySet();
    }

    public void delete() {
        for (RenderRegion region : this.regions.values()) {
            region.delete();
        }

        this.regions.clear();
    }

    public Collection<RenderRegion> getLoadedRegions() {
        return this.regions.values();
    }

    private record ChunkGeometryUpload(RenderSection section, BuiltChunkGeometry geometry, AtomicReference<BufferSegment> result) {

    }
}
