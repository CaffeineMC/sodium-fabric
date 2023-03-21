package me.jellysquid.mods.sodium.client.render.chunk.region;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectCollection;
import it.unimi.dsi.fastutil.objects.Reference2ObjectLinkedOpenHashMap;
import me.jellysquid.mods.sodium.client.SodiumClientMod;
import me.jellysquid.mods.sodium.client.gl.arena.PendingUpload;
import me.jellysquid.mods.sodium.client.gl.arena.staging.FallbackStagingBuffer;
import me.jellysquid.mods.sodium.client.gl.arena.staging.MappedStagingBuffer;
import me.jellysquid.mods.sodium.client.gl.arena.staging.StagingBuffer;
import me.jellysquid.mods.sodium.client.gl.device.CommandList;
import me.jellysquid.mods.sodium.client.gl.device.RenderDevice;
import me.jellysquid.mods.sodium.client.render.SodiumWorldRenderer;
import me.jellysquid.mods.sodium.client.render.chunk.data.SectionRenderData;
import me.jellysquid.mods.sodium.client.render.chunk.RenderSection;
import me.jellysquid.mods.sodium.client.render.chunk.compile.ChunkBuildResult;
import me.jellysquid.mods.sodium.client.render.chunk.data.BuiltSectionMeshParts;
import me.jellysquid.mods.sodium.client.render.chunk.data.BuiltSectionInfo;
import me.jellysquid.mods.sodium.client.render.chunk.terrain.DefaultTerrainRenderPasses;
import me.jellysquid.mods.sodium.client.render.chunk.terrain.TerrainRenderPass;
import net.minecraft.util.math.ChunkSectionPos;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class RenderRegionManager {
    private final Long2ObjectOpenHashMap<RenderRegion> regions = new Long2ObjectOpenHashMap<>();
    private final Long2ObjectOpenHashMap<RenderSection> sections = new Long2ObjectOpenHashMap<>();

    private final StagingBuffer stagingBuffer;

    public RenderRegionManager(CommandList commandList) {
        this.stagingBuffer = createStagingBuffer(commandList);
    }

    public void cleanup() {
        this.stagingBuffer.flip();

        try (CommandList commandList = RenderDevice.INSTANCE.createCommandList()) {
            Iterator<RenderRegion> it = this.regions.values().iterator();

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
        List<PendingSectionUpload> sectionUploads = new ArrayList<>();

        for (ChunkBuildResult result : results) {
            for (TerrainRenderPass pass : DefaultTerrainRenderPasses.ALL) {
                var storage = region.getSectionStorage(pass);

                if (storage != null) {
                    storage.deleteData(result.render.getLocalSectionIndex());
                }

                BuiltSectionMeshParts meshData = result.getMesh(pass);

                if (meshData != null) {
                    sectionUploads.add(new PendingSectionUpload(result.render, result.data, meshData, pass, new PendingUpload(meshData.getVertexData())));
                }
            }
        }

        // If we have nothing to upload, abort!
        if (sectionUploads.isEmpty()) {
            return;
        }

        var resources = region.createResources(commandList);

        boolean bufferChanged = resources.getGeometryArena()
                .upload(commandList, sectionUploads.stream().map(i -> i.vertexUpload));

        // If any of the buffers changed, the tessellation will need to be updated
        // Once invalidated the tessellation will be re-created on the next attempted use
        if (bufferChanged) {
            region.refreshPointers(commandList);
        }

        // Collect the upload results
        for (PendingSectionUpload upload : sectionUploads) {
            region.getStorage(upload.pass)
                    .replaceData(upload.section.getLocalSectionIndex(), new SectionRenderData(upload.vertexUpload.getResult(), upload.meshData));
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

            List<ChunkBuildResult> uploadQueue = map.computeIfAbsent(render.getRegion(), k -> new ArrayList<>());
            uploadQueue.add(result);
        }

        return map;
    }

    public void delete(CommandList commandList) {
        for (var region : this.regions.values()) {
            region.delete(commandList);
        }

        this.regions.clear();
        this.stagingBuffer.delete(commandList);
    }

    public ObjectCollection<RenderRegion> getLoadedRegions() {
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

    public RenderSection loadSection(@Deprecated SodiumWorldRenderer worldRenderer, int x, int y, int z) {
        RenderSection section = new RenderSection(worldRenderer, x, y, z);

        RenderRegion region = this.getOrCreateRegion(x >> RenderRegion.REGION_WIDTH_SH, y >> RenderRegion.REGION_HEIGHT_SH, z >> RenderRegion.REGION_LENGTH_SH);
        region.addChunk(section);

        section.region = region;

        this.sections.put(ChunkSectionPos.asLong(x, y, z), section);

        return section;
    }

    private RenderRegion getOrCreateRegion(int x, int y, int z) {
        var pos = ChunkSectionPos.asLong(x, y, z);

        var region = this.regions.get(pos);

        if (region == null) {
            region = this.createRegion(x, y, z, pos);
        }

        return region;
    }

    private RenderRegion createRegion(int x, int y, int z, long pos) {
        RenderRegion region = new RenderRegion(x, y, z, this.stagingBuffer);
        this.regions.put(pos, region);

        return region;
    }

    public RenderSection unloadSection(int x, int y, int z) {
        RenderSection section = this.sections.remove(ChunkSectionPos.asLong(x, y, z));
        section.region.removeChunk(section);
        section.region = null;

        return section;
    }

    private RenderRegion getRegion(int x, int y, int z) {
        return this.regions.get(ChunkSectionPos.asLong(x, y, z));
    }

    public RenderSection getSection(int x, int y, int z) {
        return this.sections.get(ChunkSectionPos.asLong(x, y, z));
    }

    public int sectionCount() {
        return this.sections.size();
    }


    public RenderRegion getByKey(long pos) {
        return this.regions.get(pos);
    }

    private record PendingSectionUpload(RenderSection section, BuiltSectionInfo renderData, BuiltSectionMeshParts meshData, TerrainRenderPass pass, PendingUpload vertexUpload) {
    }
}
