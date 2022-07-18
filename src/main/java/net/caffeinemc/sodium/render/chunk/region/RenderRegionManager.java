package net.caffeinemc.sodium.render.chunk.region;

import it.unimi.dsi.fastutil.longs.Long2ReferenceMap;
import it.unimi.dsi.fastutil.longs.Long2ReferenceOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayFIFOQueue;
import it.unimi.dsi.fastutil.objects.ReferenceArrayList;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import net.caffeinemc.gfx.api.buffer.MappedBufferFlags;
import net.caffeinemc.gfx.api.device.RenderDevice;
import net.caffeinemc.gfx.util.buffer.SectionedStreamingBuffer;
import net.caffeinemc.gfx.util.buffer.StreamingBuffer;
import net.caffeinemc.sodium.SodiumClientMod;
import net.caffeinemc.sodium.render.buffer.arena.ArenaBuffer;
import net.caffeinemc.sodium.render.buffer.arena.PendingUpload;
import net.caffeinemc.sodium.render.chunk.RenderSection;
import net.caffeinemc.sodium.render.chunk.compile.tasks.TerrainBuildResult;
import net.caffeinemc.sodium.render.chunk.state.BuiltChunkGeometry;
import net.caffeinemc.sodium.render.chunk.state.ChunkRenderData;
import net.caffeinemc.sodium.render.terrain.format.TerrainVertexType;
import net.caffeinemc.sodium.util.IntPool;

public class RenderRegionManager {
    private final Long2ReferenceMap<RenderRegion> regions = new Long2ReferenceOpenHashMap<>();
    private final IntPool idPool = new IntPool();
    
    // add to last, poll first to act like a FIFO queue
    private final Deque<ArenaBuffer> recycledVertexBuffers = new ArrayDeque<>();

    private final RenderDevice device;
    private final TerrainVertexType vertexType;
    private final StreamingBuffer stagingBuffer;

    public RenderRegionManager(RenderDevice device, TerrainVertexType vertexType) {
        this.device = device;
        this.vertexType = vertexType;

        var maxInFlightFrames = SodiumClientMod.options().advanced.cpuRenderAheadLimit + 1;
        this.stagingBuffer = new SectionedStreamingBuffer(
                device,
                1,
                0x80000, // start with 512KiB per section and expand from there if needed
                maxInFlightFrames,
                EnumSet.of(
                        MappedBufferFlags.EXPLICIT_FLUSH,
                        MappedBufferFlags.CLIENT_STORAGE
                )
        );
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
                this.recycleRegion(region);
                it.remove();
            }
        }
    }

    public void uploadChunks(Iterator<TerrainBuildResult> queue, int frameIndex, @Deprecated RenderUpdateCallback callback) {
        for (var entry : this.setupUploadBatches(queue)) {
            this.uploadGeometryBatch(entry.getLongKey(), entry.getValue(), frameIndex);

            for (TerrainBuildResult result : entry.getValue()) {
                RenderSection section = result.render();

                if (section.getData() != null) {
                    callback.accept(section, section.getData(), result.data());
                }

                section.setData(result.data());
                section.setLastAcceptedBuildTime(result.buildTime());

                result.delete();
            }
        }
    }

    public int getRegionTableSize() {
        return this.idPool.capacity();
    }

    public interface RenderUpdateCallback {
        void accept(RenderSection section, ChunkRenderData prev, ChunkRenderData next);
    }

    private void uploadGeometryBatch(long regionKey, List<TerrainBuildResult> results, int frameIndex) {
        List<PendingUpload> uploads = new ArrayList<>();
        List<ChunkGeometryUpload> jobs = new ArrayList<>(results.size());

        for (TerrainBuildResult result : results) {
            var render = result.render();
            var geometry = result.geometry();

            // De-allocate all storage for the meshes we're about to replace
            // This will allow it to be cheaply re-allocated later
            render.deleteGeometry();

            var vertices = geometry.vertices();
    
            // Only submit an upload job if there is data in the first place
            if (vertices != null) {
                var upload = new PendingUpload(vertices.buffer());
                jobs.add(new ChunkGeometryUpload(render, geometry, upload.bufferSegmentHolder));

                uploads.add(upload);
            }
        }

        // If we have nothing to upload, don't allocate a region
        if (jobs.isEmpty()) {
            return;
        }

        RenderRegion region = this.regions.get(regionKey);

        if (region == null) {
            this.regions.put(regionKey, region = this.createRegion());
        }

        region.vertexBuffers.upload(uploads, frameIndex);

        // Collect the upload results
        for (ChunkGeometryUpload upload : jobs) {
            upload.section.updateGeometry(region, upload.bufferSegmentResult.get());
        }
    }

    private Iterable<Long2ReferenceMap.Entry<List<TerrainBuildResult>>> setupUploadBatches(Iterator<TerrainBuildResult> renders) {
        var batches = new Long2ReferenceOpenHashMap<List<TerrainBuildResult>>();

        while (renders.hasNext()) {
            TerrainBuildResult result = renders.next();
            RenderSection render = result.render();

            // TODO: this is kinda gross, maybe find a way to make the Future dispose of the result when cancelled?
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
            this.deleteRegion(region);
        }
        this.regions.clear();

        this.stagingBuffer.delete();
    }

    private void deleteRegion(RenderRegion region) {
        var id = region.id;
        region.delete();

        this.idPool.free(id);
    }
    
    private void recycleRegion(RenderRegion region) {
        var id = region.id;
        region.recycle(this.recycledVertexBuffers);
        
        this.idPool.free(id);
    }
    
    private RenderRegion createRegion() {
        int id = this.idPool.create();
        
        if (!this.recycledVertexBuffers.isEmpty()) {
            return new RenderRegion(this.recycledVertexBuffers.pollLast(), id);
        } else {
            return new RenderRegion(this.device, this.stagingBuffer, this.vertexType, this.idPool.create());
        }
    }
    
    public Collection<RenderRegion> getLoadedRegions() {
        return this.regions.values();
    }
    
    public Collection<ArenaBuffer> getRecycledBuffers() {
        return this.recycledVertexBuffers;
    }

    private record ChunkGeometryUpload(RenderSection section, BuiltChunkGeometry geometry, AtomicLong bufferSegmentResult) {

    }
}
