package me.jellysquid.mods.sodium.client.render.chunk.region;

import it.unimi.dsi.fastutil.objects.Reference2ReferenceOpenHashMap;
import me.jellysquid.mods.sodium.client.gl.arena.GlBufferArena;
import me.jellysquid.mods.sodium.client.gl.arena.staging.StagingBuffer;
import me.jellysquid.mods.sodium.client.gl.buffer.GlBuffer;
import me.jellysquid.mods.sodium.client.gl.device.CommandList;
import me.jellysquid.mods.sodium.client.gl.tessellation.GlTessellation;
import me.jellysquid.mods.sodium.client.render.chunk.IndexedMap;
import me.jellysquid.mods.sodium.client.render.chunk.SharedQuadIndexBuffer;
import me.jellysquid.mods.sodium.client.render.chunk.terrain.DefaultTerrainRenderPasses;
import me.jellysquid.mods.sodium.client.render.chunk.terrain.TerrainRenderPass;
import me.jellysquid.mods.sodium.client.render.chunk.ChunkGraphicsState;
import me.jellysquid.mods.sodium.client.render.chunk.RenderSection;
import me.jellysquid.mods.sodium.client.render.chunk.vertex.format.ChunkMeshFormats;
import me.jellysquid.mods.sodium.client.util.MathUtil;
import org.apache.commons.lang3.Validate;

import java.util.EnumMap;
import java.util.Map;

public class RenderRegion implements IndexedMap.IdHolder {
    public static final int REGION_WIDTH = 8;
    public static final int REGION_HEIGHT = 4;
    public static final int REGION_LENGTH = 8;

    public static final int REGION_WIDTH_M = RenderRegion.REGION_WIDTH - 1;
    public static final int REGION_HEIGHT_M = RenderRegion.REGION_HEIGHT - 1;
    public static final int REGION_LENGTH_M = RenderRegion.REGION_LENGTH - 1;

    public static final int REGION_WIDTH_SH = Integer.bitCount(REGION_WIDTH_M);
    public static final int REGION_HEIGHT_SH = Integer.bitCount(REGION_HEIGHT_M);
    public static final int REGION_LENGTH_SH = Integer.bitCount(REGION_LENGTH_M);

    public static final int REGION_SIZE = REGION_WIDTH * REGION_HEIGHT * REGION_LENGTH;

    static {
        Validate.isTrue(MathUtil.isPowerOfTwo(REGION_WIDTH));
        Validate.isTrue(MathUtil.isPowerOfTwo(REGION_HEIGHT));
        Validate.isTrue(MathUtil.isPowerOfTwo(REGION_LENGTH));
    }

    private final int x, y, z;
    private final int id;

    private final RenderSection[] sections = new RenderSection[RenderRegion.REGION_SIZE];
    private int sectionCount;

    private final Map<TerrainRenderPass, SectionData> sectionStorage = new Reference2ReferenceOpenHashMap<>();
    @Deprecated(forRemoval = true) // reason: arenas should not hold references to the staging buffer
    private final StagingBuffer stagingBuffer;

    private Resources resources;

    public RenderRegion(int x, int y, int z, int id, StagingBuffer stagingBuffer) {
        this.x = x;
        this.y = y;
        this.z = z;

        this.id = id;
        this.stagingBuffer = stagingBuffer;

        for (var pass : DefaultTerrainRenderPasses.ALL) {
            this.sectionStorage.put(pass, new SectionData());
        }
    }

    public int getOriginX() {
        return (this.x * REGION_WIDTH) << 4;
    }

    public int getOriginY() {
        return (this.y * REGION_HEIGHT) << 4;
    }

    public int getOriginZ() {
        return (this.z * REGION_LENGTH) << 4;
    }

    public static int getChunkIndex(int x, int y, int z) {
        return x << 5 | y << 3 | z;
    }

    public void delete(CommandList commandList) {
        for (var storage : this.sectionStorage.values()) {
            storage.delete(commandList);
        }

        this.sectionStorage.clear();

        if (this.resources != null) {
            this.resources.delete(commandList);
            this.resources = null;
        }
    }

    public Resources getResources() {
        return this.resources;
    }

    public SectionData getSectionStorage(TerrainRenderPass pass) {
        return this.sectionStorage.get(pass);
    }

    public void addChunk(RenderSection section) {
        int index = section.getChunkId();

        if (this.sections[index] != null) {
            throw new RuntimeException("Tried to add a chunk section that is already present");
        }

        this.sections[index] = section;
        this.sectionCount++;
    }

    public void removeChunk(RenderSection section) {
        int index = section.getChunkId();

        if (this.sections[index] == null) {
            throw new RuntimeException("Tried to remove a chunk section that isn't present");
        }

        this.sectionStorage.forEach((pass, storage) -> {
            storage.replaceState(section, null);
        });

        this.sections[index] = null;
        this.sectionCount--;
    }

    public boolean isEmpty() {
        return this.sectionCount == 0;
    }

    public Resources createResources(CommandList commandList) {
        if (this.resources == null) {
            this.resources = new Resources(commandList, this.stagingBuffer);
        }

        return this.resources;
    }

    @Deprecated
    public SectionData getStorage(TerrainRenderPass pass) {
        return this.sectionStorage.get(pass);
    }

    public void cleanup(CommandList commandList) {
        if (this.resources != null && this.resources.isEmpty()) {
            this.resources.delete(commandList);
            this.resources = null;
        }
    }

    public int id() {
        return this.id;
    }

    public RenderSection getChunk(int x, int y, int z) {
        return this.sections[x << 5 | z << 2 | y];
    }

    public RenderSection[] getChunks() {
        return this.sections;
    }

    public RenderSection getChunk(int section) {
        return this.sections[section];
    }

    public static class SectionData {
        private final ChunkGraphicsState[] graphicsStates = new ChunkGraphicsState[RenderRegion.REGION_SIZE];

        public ChunkGraphicsState getState(RenderSection section) {
            return this.graphicsStates[section.getChunkId()];
        }

        public void replaceState(RenderSection section, ChunkGraphicsState state) {
            var id = section.getChunkId();
            var prev = this.graphicsStates[id];
            this.graphicsStates[id] = state;

            if (prev != null) {
                prev.delete();
            }
        }

        public void delete(CommandList commandList) {
            for (int i = 0; i < this.graphicsStates.length; i++) {
                ChunkGraphicsState state = this.graphicsStates[i];

                if (state != null) {
                    state.delete();

                    this.graphicsStates[i] = null;
                }
            }
        }

    }

    public static class Resources {
        protected final GlBufferArena vertexBuffers;
        private final EnumMap<SharedQuadIndexBuffer.IndexType, GlTessellation> tessellations = new EnumMap<>(SharedQuadIndexBuffer.IndexType.class);

        public Resources(CommandList commandList, StagingBuffer stagingBuffer) {
            int stride = ChunkMeshFormats.COMPACT.getVertexFormat().getStride();

            this.vertexBuffers = new GlBufferArena(commandList, REGION_SIZE * 756, stride, stagingBuffer);
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

        public void deleteTessellations(CommandList commandList) {
            for (var tessellation : this.tessellations.values()) {
                tessellation.delete(commandList);
            }

            this.tessellations.clear();
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

        public void delete(CommandList commandList) {
            this.deleteTessellations(commandList);

            this.vertexBuffers.delete(commandList);
        }

        public GlBuffer getVertexBuffer() {
            return this.vertexBuffers.getBufferObject();
        }
    }
}
