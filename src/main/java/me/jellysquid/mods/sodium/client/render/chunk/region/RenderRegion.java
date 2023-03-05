package me.jellysquid.mods.sodium.client.render.chunk.region;

import it.unimi.dsi.fastutil.objects.Reference2ReferenceOpenHashMap;
import me.jellysquid.mods.sodium.client.gl.arena.GlBufferArena;
import me.jellysquid.mods.sodium.client.gl.arena.staging.StagingBuffer;
import me.jellysquid.mods.sodium.client.gl.device.CommandList;
import me.jellysquid.mods.sodium.client.gl.tessellation.GlTessellation;
import me.jellysquid.mods.sodium.client.render.chunk.SharedQuadIndexBuffer;
import me.jellysquid.mods.sodium.client.render.chunk.terrain.TerrainRenderPass;
import me.jellysquid.mods.sodium.client.render.chunk.ChunkGraphicsState;
import me.jellysquid.mods.sodium.client.render.chunk.RenderSection;
import me.jellysquid.mods.sodium.client.render.chunk.vertex.format.ChunkMeshFormats;
import me.jellysquid.mods.sodium.client.util.MathUtil;
import net.minecraft.util.math.ChunkSectionPos;
import org.apache.commons.lang3.Validate;

import java.util.EnumMap;
import java.util.Map;

public class RenderRegion {
    public static final int REGION_WIDTH = 8;
    public static final int REGION_HEIGHT = 4;
    public static final int REGION_LENGTH = 8;

    private static final int REGION_WIDTH_M = RenderRegion.REGION_WIDTH - 1;
    private static final int REGION_HEIGHT_M = RenderRegion.REGION_HEIGHT - 1;
    private static final int REGION_LENGTH_M = RenderRegion.REGION_LENGTH - 1;

    private static final int REGION_WIDTH_SH = Integer.bitCount(REGION_WIDTH_M);
    private static final int REGION_HEIGHT_SH = Integer.bitCount(REGION_HEIGHT_M);
    private static final int REGION_LENGTH_SH = Integer.bitCount(REGION_LENGTH_M);

    public static final int REGION_SIZE = REGION_WIDTH * REGION_HEIGHT * REGION_LENGTH;

    private static final int REGION_EXCESS = 8;

    static {
        Validate.isTrue(MathUtil.isPowerOfTwo(REGION_WIDTH));
        Validate.isTrue(MathUtil.isPowerOfTwo(REGION_HEIGHT));
        Validate.isTrue(MathUtil.isPowerOfTwo(REGION_LENGTH));
    }

    private final int x, y, z;

    public final GlBufferArena vertexBuffers;


    public final Map<TerrainRenderPass, RenderRegionStorage> storage = new Reference2ReferenceOpenHashMap<>();

    public RenderRegion(CommandList commandList, StagingBuffer stagingBuffer, int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;

        int stride = ChunkMeshFormats.COMPACT.getVertexFormat().getStride();
        this.vertexBuffers = new GlBufferArena(commandList, REGION_SIZE * 756, stride, stagingBuffer);
    }

    public static RenderRegion createRegionForChunk(CommandList commandList, StagingBuffer stagingBuffer, int x, int y, int z) {
        return new RenderRegion(commandList, stagingBuffer, x >> REGION_WIDTH_SH, y >> REGION_HEIGHT_SH, z >> REGION_LENGTH_SH);
    }

    public static long getRegionKeyForChunk(int x, int y, int z) {
        return ChunkSectionPos.asLong(x >> REGION_WIDTH_SH, y >> REGION_HEIGHT_SH, z >> REGION_LENGTH_SH);
    }

    public int getOriginX() {
        return this.x << REGION_WIDTH_SH << 4;
    }

    public int getOriginY() {
        return this.y << REGION_HEIGHT_SH << 4;
    }

    public int getOriginZ() {
        return this.z << REGION_LENGTH_SH << 4;
    }

    public static int getChunkIndex(int x, int y, int z) {
        return x << 5 | y << 3 | z;
    }

    public void delete(CommandList commandList) {
        for (var storage : this.storage.values()) {
            storage.delete(commandList);
        }

        this.storage.clear();

        this.vertexBuffers.delete(commandList);
    }

    public void deleteTessellations(CommandList commandList) {
        for (var storage : this.storage.values()) {
            storage.deleteTessellation(commandList);
        }
    }

    public boolean isEmpty() {
        return this.vertexBuffers.isEmpty();
    }

    public long getDeviceUsedMemory() {
        return this.vertexBuffers.getDeviceUsedMemory();
    }

    public long getDeviceAllocatedMemory() {
        return this.vertexBuffers.getDeviceAllocatedMemory();
    }

    public RenderRegionStorage getStorage(TerrainRenderPass pass) {
        return this.storage.get(pass);
    }

    public RenderRegionStorage createStorage(TerrainRenderPass pass) {
        RenderRegionStorage storage = this.storage.get(pass);

        if (storage == null) {
            this.storage.put(pass, storage = new RenderRegionStorage());
        }

        return storage;
    }

    public void deleteSection(RenderSection chunk) {
        this.storage.forEach((pass, regionStorage) -> {
            ChunkGraphicsState state = regionStorage.graphicsStates[chunk.getChunkId()];
            if (state != null) {
                state.delete();
                regionStorage.graphicsStates[chunk.getChunkId()] = null;
            }
        });
    }

    public static class RenderRegionStorage {
        private final ChunkGraphicsState[] graphicsStates = new ChunkGraphicsState[RenderRegion.REGION_SIZE];

        private final EnumMap<SharedQuadIndexBuffer.IndexType, GlTessellation> tessellations = new EnumMap<>(SharedQuadIndexBuffer.IndexType.class);

        public ChunkGraphicsState setState(RenderSection section, ChunkGraphicsState state) {
            var id = section.getChunkId();

            var prev = this.graphicsStates[id];
            this.graphicsStates[id] = state;

            return prev;
        }

        public ChunkGraphicsState getState(RenderSection section) {
            return this.graphicsStates[section.getChunkId()];
        }

        public void updateTessellation(CommandList commandList, SharedQuadIndexBuffer.IndexType indexType, GlTessellation tessellation) {
            var prev = this.tessellations.put(indexType, tessellation);

            if (prev != null) {
                prev.delete(commandList);
            }
        }

        public GlTessellation getTessellation(SharedQuadIndexBuffer.IndexType indexType) {
            return this.tessellations.get(indexType);
        }

        public void delete(CommandList commandList) {
            this.deleteTessellation(commandList);

            for (int i = 0; i < this.graphicsStates.length; i++) {
                ChunkGraphicsState state = this.graphicsStates[i];

                if (state != null) {
                    state.delete();

                    this.graphicsStates[i] = null;
                }
            }
        }

        public void deleteTessellation(CommandList commandList) {
            for (var tessellation : this.tessellations.values()) {
                tessellation.delete(commandList);
            }

            this.tessellations.clear();
        }

        public void replaceState(RenderSection section, ChunkGraphicsState state) {
            var prev = this.setState(section, state);

            if (prev != null) {
                prev.delete();
            }
        }
    }
}
