package me.jellysquid.mods.sodium.client.render.chunk.region;

import it.unimi.dsi.fastutil.longs.Long2ReferenceOpenHashMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectLinkedOpenHashMap;
import me.jellysquid.mods.sodium.client.SodiumClientMod;
import me.jellysquid.mods.sodium.client.gl.arena.GlBufferSegment;
import me.jellysquid.mods.sodium.client.gl.arena.staging.FallbackStagingBuffer;
import me.jellysquid.mods.sodium.client.gl.arena.staging.MappedStagingBuffer;
import me.jellysquid.mods.sodium.client.gl.arena.staging.StagingBuffer;
import me.jellysquid.mods.sodium.client.gl.device.CommandList;
import me.jellysquid.mods.sodium.client.gl.device.RenderDevice;
import me.jellysquid.mods.sodium.client.render.chunk.ChunkGraphicsState;
import me.jellysquid.mods.sodium.client.render.chunk.RenderSection;
import me.jellysquid.mods.sodium.client.render.chunk.compile.ChunkBuildResult;
import me.jellysquid.mods.sodium.client.render.chunk.format.ChunkMesh;
import me.jellysquid.mods.sodium.client.render.chunk.passes.BlockRenderLayer;
import me.jellysquid.mods.sodium.client.render.chunk.passes.BlockRenderLayerManager;
import me.jellysquid.mods.sodium.client.render.chunk.passes.ChunkMeshType;
import me.jellysquid.mods.sodium.client.util.frustum.Frustum;

import java.util.*;

public class RenderRegionManager {
    private final Long2ReferenceOpenHashMap<RenderRegion> regions = new Long2ReferenceOpenHashMap<>();

    private final StagingBuffer stagingBuffer;
    private final BlockRenderLayerManager renderLayers;

    public RenderRegionManager(CommandList commandList, BlockRenderLayerManager renderLayers) {
        this.stagingBuffer = createStagingBuffer(commandList);
        this.renderLayers = renderLayers;
    }

    public void updateVisibility(Frustum frustum) {
        for (RenderRegion region : this.regions.values()) {
            if (!region.isEmpty()) {
                region.updateVisibility(frustum);
            }
        }
    }

    public void cleanup() {
        this.stagingBuffer.flip();

        try (CommandList commandList = RenderDevice.INSTANCE.createCommandList()) {
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

    public void upload(CommandList commandList, Iterator<ChunkBuildResult> queue) {
        for (Map.Entry<RenderRegion, List<ChunkBuildResult>> entry : this.setupUploadBatches(queue).entrySet()) {
            RenderRegion region = entry.getKey();
            List<ChunkBuildResult> uploadQueue = entry.getValue();

            this.upload(commandList, region, uploadQueue);

            for (ChunkBuildResult result : uploadQueue) {
                result.render.onBuildFinished(result);

                result.delete();
            }
        }
    }

    private void upload(CommandList commandList, RenderRegion region, List<ChunkBuildResult> results) {
        for (ChunkBuildResult result : results) {
            result.render.deleteGraphicsState();

            for (var layerEntry : result.getMeshes()) {
                var layer = layerEntry.getKey();
                var meshes = layerEntry.getValue();

                this.uploadMesh(commandList, region, ChunkMeshType.MODEL, layer, meshes.get(ChunkMeshType.MODEL), result.render);
                this.uploadMesh(commandList, region, ChunkMeshType.CUBE, layer, meshes.get(ChunkMeshType.CUBE), result.render);
            }
        }
    }

    private <E extends Enum<E> & ChunkMeshType.StorageBufferTarget> void uploadMesh(CommandList commandList, RenderRegion region, ChunkMeshType<E> meshType,
                                                                                    BlockRenderLayer renderLayer, ChunkMesh mesh, RenderSection section) {
        if (mesh == null) {
            return;
        }

        var storage = region.getOrCreateStorage(meshType, commandList);

        var meshBuffers = mesh.buffers();
        var uploadedBuffers = new EnumMap<E, GlBufferSegment>(meshType.getStorageType());

        for (var buffer : meshBuffers.getStorageBuffers()) {
            @SuppressWarnings("unchecked")
            var target = (E) buffer.getKey();
            var payload = buffer.getValue();

            var segment = storage.getArena(target)
                    .upload(commandList, payload.getDirectBuffer());

            uploadedBuffers.put(target, segment);
        }

        storage.setChunkState(section.getLocalIndex(), renderLayer, new ChunkGraphicsState<>(uploadedBuffers, mesh.parts()));
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

    public void delete(CommandList commandList) {
        for (RenderRegion region : this.regions.values()) {
            region.deleteResources(commandList);
        }

        this.regions.clear();
        this.stagingBuffer.delete(commandList);
    }

    public Collection<RenderRegion> getLoadedRegions() {
        return this.regions.values();
    }

    public StagingBuffer getStagingBuffer() {
        return this.stagingBuffer;
    }

    protected <E extends Enum<E> & ChunkMeshType.StorageBufferTarget> RenderRegionStorage<E> createRegionStorage(ChunkMeshType<E> meshType, CommandList commandList) {
        return new RenderRegionStorage<>(meshType, commandList, this.stagingBuffer, this.renderLayers);
    }

    private static StagingBuffer createStagingBuffer(CommandList commandList) {
        if (SodiumClientMod.options().advanced.useAdvancedStagingBuffers && MappedStagingBuffer.isSupported(RenderDevice.INSTANCE)) {
            return new MappedStagingBuffer(commandList);
        }

        return new FallbackStagingBuffer(commandList);
    }

}
