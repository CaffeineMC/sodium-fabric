package me.jellysquid.mods.sodium.client.render.chunk.region;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ReferenceOpenHashMap;
import me.jellysquid.mods.sodium.client.SodiumClientMod;
import me.jellysquid.mods.sodium.client.gl.arena.PendingUpload;
import me.jellysquid.mods.sodium.client.gl.arena.staging.FallbackStagingBuffer;
import me.jellysquid.mods.sodium.client.gl.arena.staging.MappedStagingBuffer;
import me.jellysquid.mods.sodium.client.gl.arena.staging.StagingBuffer;
import me.jellysquid.mods.sodium.client.gl.buffer.IndexedVertexData;
import me.jellysquid.mods.sodium.client.gl.device.CommandList;
import me.jellysquid.mods.sodium.client.gl.device.RenderDevice;
import me.jellysquid.mods.sodium.client.render.chunk.ChunkGraphicsState;
import me.jellysquid.mods.sodium.client.render.chunk.RenderSection;
import me.jellysquid.mods.sodium.client.render.chunk.compile.ChunkBuildResult;
import me.jellysquid.mods.sodium.client.render.chunk.data.ChunkMeshData;
import me.jellysquid.mods.sodium.client.render.chunk.passes.BlockRenderPass;
import net.minecraft.util.math.ChunkSectionPos;

import java.util.*;

public class RenderRegionManager {
    private final Long2ReferenceOpenHashMap<RenderRegion> regions = new Long2ReferenceOpenHashMap<>();

    private final StagingBuffer stagingBuffer;

    public RenderRegionManager(CommandList commandList) {
        this.stagingBuffer = createStagingBuffer(commandList);
    }

    public void cleanup() {
        this.stagingBuffer.flip();

        try (CommandList commandList = RenderDevice.INSTANCE.createCommandList()) {
            Iterator<RenderRegion> it = this.regions.values()
                    .iterator();

            while (it.hasNext()) {
                RenderRegion region = it.next();

                if (region.isEmpty()) {
                    region.delete(commandList);

                    it.remove();
                }
            }
        }
    }

    public void uploadMeshes(CommandList commandList, ArrayList<ChunkBuildResult> results) {
        var batches = this.setupUploadBatches(results);

        for (var entry : batches.long2ObjectEntrySet()) {
            this.uploadMeshes(commandList, entry.getLongKey(), entry.getValue());
        }
    }

    private void uploadMeshes(CommandList commandList, long regionId, List<PendingSectionUpload> uploads) {
        var region = this.getOrCreateRegion(regionId);

        boolean buffersResized = region.getVertexBuffer().upload(commandList, uploads.stream().map(i -> i.vertexUpload));
        buffersResized |= region.getIndexBuffer().upload(commandList, uploads.stream().map(i -> i.indicesUpload));

        // If any of the buffers changed, the tessellation will need to be updated
        // Once invalidated the tessellation will be re-created on the next attempted use
        if (buffersResized) {
            region.deleteTessellations(commandList);
        }

        // Collect the upload results
        for (PendingSectionUpload upload : uploads) {
            region.getData(upload.pass)
                    .setGraphicsState(upload.section.getLocalId(), new ChunkGraphicsState(upload.vertexUpload.getResult(), upload.indicesUpload.getResult(), upload.meshData));
        }
    }

    private Long2ObjectMap<List<PendingSectionUpload>> setupUploadBatches(List<ChunkBuildResult> results) {
        Long2ObjectMap<List<PendingSectionUpload>> map = new Long2ObjectOpenHashMap<>();

        for (ChunkBuildResult result : results) {
            var region = this.regions.get(result.section.getRegionId());

            if (region != null) {
                region.deleteChunk(result.section.getLocalId());
            }

            for (BlockRenderPass pass : BlockRenderPass.VALUES) {
                ChunkMeshData meshData = result.getMesh(pass);

                if (meshData != null) {
                    IndexedVertexData vertexData = meshData.getVertexData();

                    List<PendingSectionUpload> uploadQueue = map.computeIfAbsent(result.section.getRegionId(), key -> new ArrayList<>());
                    uploadQueue.add(new RenderRegionManager.PendingSectionUpload(
                            result.section,
                            meshData,
                            pass,
                            new PendingUpload(vertexData.vertexBuffer()),
                            new PendingUpload(vertexData.indexBuffer())
                    ));
                }
            }
        }

        return map;
    }

    private RenderRegion getOrCreateRegion(long key) {
        RenderRegion region = this.regions.get(key);

        if (region == null) {
            CommandList commandList = RenderDevice.INSTANCE.createCommandList();
            this.regions.put(key, region = new RenderRegion(ChunkSectionPos.unpackX(key), ChunkSectionPos.unpackY(key), ChunkSectionPos.unpackZ(key),
                    commandList, this.stagingBuffer));
        }

        return region;
    }

    public void delete(CommandList commandList) {
        for (RenderRegion region : this.regions.values()) {
            region.delete(commandList);
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


    private static StagingBuffer createStagingBuffer(CommandList commandList) {
        if (SodiumClientMod.options().advanced.useAdvancedStagingBuffers && MappedStagingBuffer.isSupported(RenderDevice.INSTANCE)) {
            return new MappedStagingBuffer(commandList);
        }

        return new FallbackStagingBuffer(commandList);
    }

    public RenderRegion getRegion(long id) {
        return this.regions.get(id);
    }

    public record PendingSectionUpload(RenderSection section,
                                       ChunkMeshData meshData,
                                       BlockRenderPass pass,
                                       PendingUpload vertexUpload,
                                       PendingUpload indicesUpload) {
    }
}
