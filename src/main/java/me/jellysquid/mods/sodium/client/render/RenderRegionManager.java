package me.jellysquid.mods.sodium.client.render;

import it.unimi.dsi.fastutil.longs.Long2ReferenceOpenHashMap;
import it.unimi.dsi.fastutil.objects.*;
import me.jellysquid.mods.sodium.client.SodiumClientMod;
import me.jellysquid.mods.sodium.client.gl.arena.GlBufferArena;
import me.jellysquid.mods.sodium.client.gl.arena.GlBufferSegment;
import me.jellysquid.mods.sodium.client.gl.buffer.IndexedVertexData;
import me.jellysquid.mods.sodium.client.gl.device.CommandList;
import me.jellysquid.mods.sodium.client.gl.device.RenderDevice;
import me.jellysquid.mods.sodium.client.render.chunk.RenderChunk;
import me.jellysquid.mods.sodium.client.render.chunk.backend.multidraw.ChunkGraphicsState;
import me.jellysquid.mods.sodium.client.render.chunk.compile.ChunkBuildResult;
import me.jellysquid.mods.sodium.client.render.chunk.data.ChunkMeshData;
import me.jellysquid.mods.sodium.client.render.chunk.data.ChunkRenderData;
import me.jellysquid.mods.sodium.client.render.chunk.passes.BlockRenderPass;
import net.minecraft.util.math.ChunkSectionPos;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class RenderRegionManager {
    private static final int BUFFER_WIDTH_M = RenderRegion.REGION_WIDTH - 1;
    private static final int BUFFER_HEIGHT_M = RenderRegion.REGION_HEIGHT - 1;
    private static final int BUFFER_LENGTH_M = RenderRegion.REGION_LENGTH - 1;

    private static final int BUFFER_WIDTH_SH = Integer.bitCount(BUFFER_WIDTH_M);
    private static final int BUFFER_HEIGHT_SH = Integer.bitCount(BUFFER_HEIGHT_M);
    private static final int BUFFER_LENGTH_SH = Integer.bitCount(BUFFER_LENGTH_M);

    private final Long2ReferenceOpenHashMap<RenderRegion> regions = new Long2ReferenceOpenHashMap<>();

    private final Reference2ObjectMap<RenderRegion, List<ChunkBuildResult>> uploadQueue = new Reference2ObjectOpenHashMap<>();

    private final RenderDevice device;

    public RenderRegionManager(RenderDevice device) {
        this.device = device;
    }

    public void upload(CommandList commandList, Iterator<ChunkBuildResult> queue) {
        this.setupUploadBatches(queue);

        for (Map.Entry<RenderRegion, List<ChunkBuildResult>> entry : this.uploadQueue.entrySet()) {
            RenderRegion region = entry.getKey();
            List<ChunkBuildResult> uploadQueue = entry.getValue();

            this.upload(commandList, region, uploadQueue);
        }

        this.uploadQueue.clear();
    }

    private void upload(CommandList commandList, RenderRegion region, List<ChunkBuildResult> uploadQueue) {
        GlBufferArena indexBuffer = region.getIndexBufferArena();

        GlBufferArena vertexBuffer = region.getVertexBufferArena();
        vertexBuffer.checkArenaCapacity(commandList, getUploadQueuePayloadSize(uploadQueue));

        for (ChunkBuildResult result : uploadQueue) {
            RenderChunk render = result.render;
            ChunkRenderData data = result.data;

            for (BlockRenderPass pass : BlockRenderPass.VALUES) {
                ChunkGraphicsState graphics = render.getGraphicsState(pass);

                // De-allocate the existing buffer arena for this render
                // This will allow it to be cheaply re-allocated just below
                if (graphics != null) {
                    graphics.delete();
                }

                ChunkMeshData meshData = data.getMesh(pass);

                if (meshData != null && meshData.hasVertexData()) {
                    IndexedVertexData upload = meshData.takeVertexData();

                    GlBufferSegment vertexSegment = vertexBuffer.uploadBuffer(commandList, upload.vertexBuffer);
                    GlBufferSegment indexSegment = indexBuffer.uploadBuffer(commandList, upload.indexBuffer);

                    render.setGraphicsState(pass, new ChunkGraphicsState(render, region, vertexSegment, indexSegment, meshData));
                } else {
                    render.setGraphicsState(pass, null);
                }
            }

            render.setData(data);
        }

        if (region.getTessellation() != null) {
            region.getTessellation()
                    .delete(commandList);
            region.setTessellation(null);
        }
    }

    private void setupUploadBatches(Iterator<ChunkBuildResult> renders) {
        while (renders.hasNext()) {
            ChunkBuildResult result = renders.next();
            RenderChunk render = result.render;

            if (render.isDisposed()) {
                SodiumClientMod.logger().warn("Tried to upload meshes for chunk " + result.render + ", but it has already been disposed");
                continue;
            }

            RenderRegion region = this.regions.get(getRegionKey(render.getChunkX(), render.getChunkY(), render.getChunkZ()));

            if (region == null) {
                throw new NullPointerException("Couldn't find region for chunk: " + render);
            }

            List<ChunkBuildResult> uploadQueue = this.uploadQueue.get(region);

            if (uploadQueue == null) {
                this.uploadQueue.put(region, uploadQueue = new ArrayList<>());
            }

            uploadQueue.add(result);
        }
    }

    private static int getUploadQueuePayloadSize(List<ChunkBuildResult> queue) {
        int size = 0;

        for (ChunkBuildResult result : queue) {
            size += result.data.getMeshSize();
        }

        return size;
    }

    public RenderChunk createChunk(SodiumWorldRenderer renderer, int x, int y, int z) {
        RenderRegion region = this.createRegionForChunk(x, y, z);
        RenderChunk chunk = region.getChunk(x & BUFFER_WIDTH_M, y & BUFFER_HEIGHT_M, z & BUFFER_LENGTH_M);

        if (chunk == null) {
            chunk = new RenderChunk(renderer, x, y, z, region);

            region.addRender(x & BUFFER_WIDTH_M, y & BUFFER_HEIGHT_M, z & BUFFER_LENGTH_M, chunk);
        }

        return chunk;
    }

    public RenderChunk getChunk(int x, int y, int z) {
        RenderRegion region = this.regions.get(getRegionKey(x, y, z));

        if (region == null) {
            return null;
        }

        return region.getChunk(x & BUFFER_WIDTH_M, y & BUFFER_HEIGHT_M, z & BUFFER_LENGTH_M);
    }

    public RenderChunk removeChunk(int x, int y, int z) {
        long key = getRegionKey(x, y, z);
        RenderRegion region = this.regions.get(key);

        if (region == null) {
            return null;
        }

        RenderChunk chunk = region.removeChunk(x & BUFFER_WIDTH_M, y & BUFFER_HEIGHT_M, z & BUFFER_LENGTH_M);

        if (region.getChunkCount() <= 0) {
            region.deleteResources();

            this.regions.remove(key);
        }

        return chunk;
    }

    private RenderRegion createRegionForChunk(int x, int y, int z) {
        long key = getRegionKey(x, y, z);

        RenderRegion region = this.regions.get(key);

        if (region == null) {
            this.regions.put(key, region = new RenderRegion(this.device));
        }

        return region;
    }

    public static long getRegionKey(int x, int y, int z) {
        return ChunkSectionPos.asLong(x >> BUFFER_WIDTH_SH, y >> BUFFER_HEIGHT_SH, z >> BUFFER_LENGTH_SH);
    }

    public void delete() {
        for (RenderRegion region : this.regions.values()) {
            region.deleteResources();
        }

        this.regions.clear();
    }

    public int getLoadedChunks() {
        return this.regions.values()
                .stream()
                .mapToInt(RenderRegion::getChunkCount)
                .sum();
    }
}
