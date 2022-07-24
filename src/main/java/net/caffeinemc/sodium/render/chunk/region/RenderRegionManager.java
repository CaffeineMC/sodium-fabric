package net.caffeinemc.sodium.render.chunk.region;

import it.unimi.dsi.fastutil.longs.Long2ReferenceMap;
import it.unimi.dsi.fastutil.longs.Long2ReferenceOpenHashMap;
import it.unimi.dsi.fastutil.objects.ReferenceArrayList;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import net.caffeinemc.gfx.api.buffer.ImmutableBuffer;
import net.caffeinemc.gfx.api.buffer.ImmutableBufferFlags;
import net.caffeinemc.gfx.api.buffer.MappedBufferFlags;
import net.caffeinemc.gfx.api.device.RenderDevice;
import net.caffeinemc.gfx.util.buffer.streaming.SectionedStreamingBuffer;
import net.caffeinemc.gfx.util.buffer.streaming.StreamingBuffer;
import net.caffeinemc.sodium.SodiumClientMod;
import net.caffeinemc.gfx.util.buffer.BufferPool;
import net.caffeinemc.sodium.render.buffer.arena.PendingUpload;
import net.caffeinemc.sodium.render.chunk.RenderSection;
import net.caffeinemc.sodium.render.chunk.compile.tasks.TerrainBuildResult;
import net.caffeinemc.sodium.render.chunk.state.BuiltChunkGeometry;
import net.caffeinemc.sodium.render.chunk.state.ChunkRenderData;
import net.caffeinemc.sodium.render.terrain.format.TerrainVertexType;
import net.caffeinemc.sodium.util.IntPool;

public class RenderRegionManager {
    // both found from experimentation
    private static final double PRUNE_RATIO_THRESHOLD = .35;
    private static final float PRUNE_PERCENT_MODIFIER = -.2f;
    
    private final Long2ReferenceMap<RenderRegion> regions = new Long2ReferenceOpenHashMap<>();
    private final IntPool idPool = new IntPool();
    private final BufferPool<ImmutableBuffer> bufferPool;

    private final RenderDevice device;
    private final TerrainVertexType vertexType;
    private final StreamingBuffer stagingBuffer;

    public RenderRegionManager(RenderDevice device, TerrainVertexType vertexType) {
        this.device = device;
        this.vertexType = vertexType;
    
        this.bufferPool = new BufferPool<>(
                device,
                100,
                c -> device.createBuffer(
                        c,
                        EnumSet.noneOf(ImmutableBufferFlags.class)
                )
        );

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
                this.deleteRegion(region);
                it.remove();
            }
        }
    
        long activeSize = this.getDeviceAllocatedMemoryActive();
        long reserveSize = this.bufferPool.getDeviceAllocatedMemory();
        
        if ((double) reserveSize / activeSize > PRUNE_RATIO_THRESHOLD) {
            this.prune();
        }
    }
    
    public void prune() {
        this.bufferPool.prune(PRUNE_PERCENT_MODIFIER);
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
            region = new RenderRegion(
                    this.device,
                    this.stagingBuffer,
                    this.bufferPool,
                    this.vertexType,
                    this.idPool.create()
            );
            
            this.regions.put(regionKey, region);
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
            region.delete();
        }
        this.regions.clear();
        
        this.bufferPool.delete();
        this.stagingBuffer.delete();
    }

    private void deleteRegion(RenderRegion region) {
        var id = region.id;
        region.delete();

        this.idPool.free(id);
    }
    
    private long getDeviceAllocatedMemoryActive() {
        return this.regions.values().stream().mapToLong(RenderRegion::getDeviceAllocatedMemory).sum();
    }
    
    public int getDeviceBufferObjects() {
        return this.regions.size() + this.bufferPool.getDeviceBufferObjects();
    }
    
    public long getDeviceUsedMemory() {
        return this.regions.values().stream().mapToLong(RenderRegion::getDeviceUsedMemory).sum();
    }
    
    public long getDeviceAllocatedMemory() {
        return this.getDeviceAllocatedMemoryActive() + this.bufferPool.getDeviceAllocatedMemory();
    }

    private record ChunkGeometryUpload(RenderSection section, BuiltChunkGeometry geometry, AtomicLong bufferSegmentResult) {

    }
}
