package me.jellysquid.mods.sodium.client.render.chunk.region;

import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import it.unimi.dsi.fastutil.objects.Reference2ReferenceOpenHashMap;
import me.jellysquid.mods.sodium.client.SodiumClientMod;
import me.jellysquid.mods.sodium.client.gl.arena.AsyncBufferArena;
import me.jellysquid.mods.sodium.client.gl.arena.GlBufferArena;
import me.jellysquid.mods.sodium.client.gl.arena.SwapBufferArena;
import me.jellysquid.mods.sodium.client.gl.arena.staging.StagingBuffer;
import me.jellysquid.mods.sodium.client.gl.device.CommandList;
import me.jellysquid.mods.sodium.client.gl.tessellation.GlTessellation;
import me.jellysquid.mods.sodium.client.render.chunk.passes.RenderPass;
import me.jellysquid.mods.sodium.client.render.chunk.ChunkGraphicsState;
import me.jellysquid.mods.sodium.client.render.chunk.RenderSection;
import me.jellysquid.mods.sodium.client.render.chunk.vertex.format.ChunkMeshFormats;
import me.jellysquid.mods.sodium.client.util.MathUtil;
import me.jellysquid.mods.sodium.client.util.frustum.Frustum;
import net.minecraft.util.math.ChunkSectionPos;
import org.apache.commons.lang3.Validate;

import java.util.Map;
import java.util.Set;

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

    public final GlBufferArena indexBuffers;

    public final Map<RenderPass, RenderRegionStorage> storage = new Reference2ReferenceOpenHashMap<>();

    public RenderRegion(CommandList commandList, StagingBuffer stagingBuffer, int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;

        int expectedVertexCount = REGION_SIZE * 756;
        int expectedIndexCount = (expectedVertexCount / 4) * 6;

        this.vertexBuffers = createArena(commandList, expectedVertexCount, ChunkMeshFormats.COMPACT.getVertexFormat().getStride(), stagingBuffer);
        this.indexBuffers = createArena(commandList, expectedIndexCount, Integer.BYTES, stagingBuffer);
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
        this.indexBuffers.delete(commandList);
    }

    public void deleteTessellations(CommandList commandList) {
        for (var storage : this.storage.values()) {
            storage.deleteTessellation(commandList);
        }
    }

    public boolean isEmpty() {
        return this.vertexBuffers.isEmpty() && this.indexBuffers.isEmpty();
    }

    public long getDeviceUsedMemory() {
        return this.vertexBuffers.getDeviceUsedMemory() + this.indexBuffers.getDeviceUsedMemory();
    }

    public long getDeviceAllocatedMemory() {
        return this.vertexBuffers.getDeviceAllocatedMemory() + this.indexBuffers.getDeviceAllocatedMemory();
    }

    private static GlBufferArena createArena(CommandList commandList, int initialCapacity, int stride, StagingBuffer stagingBuffer) {
        return switch (SodiumClientMod.options().advanced.arenaMemoryAllocator) {
            case ASYNC -> new AsyncBufferArena(commandList, initialCapacity, stride, stagingBuffer);
            case SWAP -> new SwapBufferArena(commandList, stride);
        };
    }

    public RenderRegionStorage getStorage(RenderPass pass) {
        return this.storage.get(pass);
    }

    public RenderRegionStorage createStorage(RenderPass pass) {
        RenderRegionStorage storage = this.storage.get(pass);

        if (storage == null) {
            this.storage.put(pass, storage = new RenderRegionStorage());
        }

        return storage;
    }

    public static class RenderRegionStorage {
        private final ChunkGraphicsState[] graphicsStates = new ChunkGraphicsState[RenderRegion.REGION_SIZE];

        private GlTessellation tessellation;

        public ChunkGraphicsState setState(RenderSection section, ChunkGraphicsState state) {
            var id = section.getChunkId();

            var prev = this.graphicsStates[id];
            this.graphicsStates[id] = state;

            return prev;
        }

        public ChunkGraphicsState getState(RenderSection section) {
            return this.graphicsStates[section.getChunkId()];
        }

        public void setTessellation(GlTessellation tessellation) {
            this.tessellation = tessellation;
        }

        public GlTessellation getTessellation() {
            return this.tessellation;
        }

        public void delete(CommandList commandList) {
            this.deleteTessellation(commandList);

            for (ChunkGraphicsState state : this.graphicsStates) {
                if (state != null) {
                    state.delete();
                }
            }
        }

        public void deleteTessellation(CommandList commandList) {
            if (this.tessellation != null) {
                this.tessellation.delete(commandList);
                this.tessellation = null;
            }
        }

        public void deleteState(RenderSection chunk) {
            var state = this.setState(chunk, null);

            if (state != null) {
                state.delete();
            }
        }
    }
}
