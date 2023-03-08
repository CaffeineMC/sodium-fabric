package me.jellysquid.mods.sodium.client.render.chunk.region;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectLinkedOpenHashMap;
import me.jellysquid.mods.sodium.client.SodiumClientMod;
import me.jellysquid.mods.sodium.client.gl.arena.PendingUpload;
import me.jellysquid.mods.sodium.client.gl.arena.staging.FallbackStagingBuffer;
import me.jellysquid.mods.sodium.client.gl.arena.staging.MappedStagingBuffer;
import me.jellysquid.mods.sodium.client.gl.arena.staging.StagingBuffer;
import me.jellysquid.mods.sodium.client.gl.device.CommandList;
import me.jellysquid.mods.sodium.client.gl.device.RenderDevice;
import me.jellysquid.mods.sodium.client.render.SodiumWorldRenderer;
import me.jellysquid.mods.sodium.client.render.chunk.IndexedMap;
import me.jellysquid.mods.sodium.client.render.chunk.terrain.DefaultTerrainRenderPasses;
import me.jellysquid.mods.sodium.client.render.chunk.terrain.TerrainRenderPass;
import me.jellysquid.mods.sodium.client.render.chunk.ChunkGraphicsState;
import me.jellysquid.mods.sodium.client.render.chunk.RenderSection;
import me.jellysquid.mods.sodium.client.render.chunk.compile.ChunkBuildResult;
import me.jellysquid.mods.sodium.client.render.chunk.data.ChunkMeshData;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.util.math.MathHelper;

import java.util.*;

public class RenderRegionManager {
    private final IndexedMap<RenderRegion> regions;

    private final StagingBuffer stagingBuffer;

    private final Long2ObjectOpenHashMap<RenderSection> sections = new Long2ObjectOpenHashMap<>();

    public RenderRegionManager(CommandList commandList) {
        this.stagingBuffer = createStagingBuffer(commandList);
        this.regions = new IndexedMap<>(RenderRegion.class, (x, y, z, id) -> new RenderRegion(x, y, z, id, this.stagingBuffer));
    }

    public void cleanup() {
        this.stagingBuffer.flip();

        try (CommandList commandList = RenderDevice.INSTANCE.createCommandList()) {
            Iterator<RenderRegion> it = this.regions.iterator();

            while (it.hasNext()) {
                RenderRegion region = it.next();
                region.cleanup(commandList);

                if (region.isEmpty()) {
                    it.remove();
                }
            }
        }
    }

    public void upload(CommandList commandList, Iterator<ChunkBuildResult> queue) {
        for (Map.Entry<RenderRegion, List<ChunkBuildResult>> entry : this.setupUploadBatches(commandList, queue).entrySet()) {
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
        List<PendingSectionUpload> sectionUploads = new ArrayList<>();

        for (ChunkBuildResult result : results) {
            for (TerrainRenderPass pass : DefaultTerrainRenderPasses.ALL) {
                var storage = region.getSectionStorage(pass);

                if (storage != null) {
                    storage.replaceState(result.render, null);
                }

                ChunkMeshData meshData = result.getMesh(pass);

                if (meshData != null) {
                    sectionUploads.add(new PendingSectionUpload(result.render, meshData, pass, new PendingUpload(meshData.getVertexData())));
                }
            }
        }

        // If we have nothing to upload, abort!
        if (sectionUploads.isEmpty()) {
            return;
        }

        var resources = region.createResources(commandList);

        boolean bufferChanged = resources.vertexBuffers.upload(commandList, sectionUploads.stream().map(i -> i.vertexUpload));

        // If any of the buffers changed, the tessellation will need to be updated
        // Once invalidated the tessellation will be re-created on the next attempted use
        if (bufferChanged) {
            resources.deleteTessellations(commandList);
        }

        // Collect the upload results
        for (PendingSectionUpload upload : sectionUploads) {
            region.getStorage(upload.pass)
                    .replaceState(upload.section, new ChunkGraphicsState(upload.vertexUpload.getResult(), upload.meshData));
        }
    }

    private Map<RenderRegion, List<ChunkBuildResult>> setupUploadBatches(CommandList commandList, Iterator<ChunkBuildResult> renders) {
        Map<RenderRegion, List<ChunkBuildResult>> map = new Reference2ObjectLinkedOpenHashMap<>();

        while (renders.hasNext()) {
            ChunkBuildResult result = renders.next();
            RenderSection render = result.render;

            if (!render.canAcceptBuildResults(result)) {
                result.delete();

                continue;
            }

            List<ChunkBuildResult> uploadQueue = map.computeIfAbsent(render.getRegion(), k -> new ArrayList<>());
            uploadQueue.add(result);
        }

        return map;
    }

    public void delete(CommandList commandList) {
        var it = this.regions.iterator();

        while (it.hasNext()) {
            var region = it.next();
            region.delete(commandList);
        }

        this.regions.clear();
        this.stagingBuffer.delete(commandList);
    }

    public Iterator<RenderRegion> getLoadedRegions() {
        return this.regions.iterator();
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

    public RenderSection loadSection(@Deprecated SodiumWorldRenderer worldRenderer, int x, int y, int z) {
        RenderRegion region = this.regions.getOrCreate(x >> RenderRegion.REGION_WIDTH_SH, y >> RenderRegion.REGION_HEIGHT_SH, z >> RenderRegion.REGION_LENGTH_SH);

        RenderSection section = new RenderSection(worldRenderer, x, y, z);
        region.addChunk(section);

        section.region = region;

        this.sections.put(ChunkSectionPos.asLong(x, y, z), section);

        return section;
    }

    public RenderSection unloadSection(int x, int y, int z) {
        RenderSection section = this.sections.remove(ChunkSectionPos.asLong(x, y, z));
        section.region.removeChunk(section);
        section.region = null;

        return section;
    }

    public RenderSection getSection(int x, int y, int z) {
        return this.sections.get(ChunkSectionPos.asLong(x, y, z));
    }

    public IndexedMap<RenderRegion> getStorage() {
        return this.regions;
    }

    private record PendingSectionUpload(RenderSection section, ChunkMeshData meshData, TerrainRenderPass pass, PendingUpload vertexUpload) {
    }
}
