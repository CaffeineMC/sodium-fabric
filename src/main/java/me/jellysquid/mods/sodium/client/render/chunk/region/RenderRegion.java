package me.jellysquid.mods.sodium.client.render.chunk.region;

import it.unimi.dsi.fastutil.objects.Reference2ReferenceOpenHashMap;
import me.jellysquid.mods.sodium.client.gl.arena.GlBufferArena;
import me.jellysquid.mods.sodium.client.gl.arena.staging.StagingBuffer;
import me.jellysquid.mods.sodium.client.gl.buffer.GlBuffer;
import me.jellysquid.mods.sodium.client.gl.device.CommandList;
import me.jellysquid.mods.sodium.client.gl.tessellation.GlTessellation;
import me.jellysquid.mods.sodium.client.render.chunk.RenderSection;
import me.jellysquid.mods.sodium.client.render.chunk.data.SectionRenderDataStorage;
import me.jellysquid.mods.sodium.client.render.chunk.data.BuiltSectionInfo;
import me.jellysquid.mods.sodium.client.render.chunk.graph.GraphNodeStorage;
import me.jellysquid.mods.sodium.client.render.chunk.terrain.DefaultTerrainRenderPasses;
import me.jellysquid.mods.sodium.client.render.chunk.terrain.TerrainRenderPass;
import me.jellysquid.mods.sodium.client.render.chunk.vertex.format.ChunkMeshFormats;
import me.jellysquid.mods.sodium.client.util.MathUtil;
import me.jellysquid.mods.sodium.client.util.frustum.Frustum;
import net.minecraft.util.math.ChunkSectionPos;
import org.apache.commons.lang3.Validate;

import java.util.Map;

public class RenderRegion {
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

    public final RenderSection[] sections = new RenderSection[RenderRegion.REGION_SIZE];
    public final GraphNodeStorage graphData;

    private int sectionCount;

    private final Map<TerrainRenderPass, SectionRenderDataStorage> sectionStorage = new Reference2ReferenceOpenHashMap<>();
    @Deprecated(forRemoval = true) // reason: arenas should not hold references to the staging buffer
    private final StagingBuffer stagingBuffer;

    private Resources resources;

    public RenderRegion(int x, int y, int z, StagingBuffer stagingBuffer) {
        this.x = x;
        this.y = y;
        this.z = z;

        this.stagingBuffer = stagingBuffer;

        for (var pass : DefaultTerrainRenderPasses.ALL) {
            this.sectionStorage.put(pass, new SectionRenderDataStorage());
        }

        this.graphData = new GraphNodeStorage();
    }

    public int getWorldX() {
        return this.getChunkX() << 4;
    }

    public int getWorldY() {
        return this.getChunkY() << 4;
    }

    public int getWorldZ() {
        return this.getChunkZ() << 4;
    }

    public void delete(CommandList commandList) {
        for (var storage : this.sectionStorage.values()) {
            storage.delete();
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

    public SectionRenderDataStorage getSectionStorage(TerrainRenderPass pass) {
        return this.sectionStorage.get(pass);
    }

    public void addChunk(RenderSection section) {
        int index = section.getLocalSectionIndex();

        if (this.sections[index] != null) {
            throw new RuntimeException("Tried to add a chunk section that is already present");
        }

        this.sections[index] = section;
        this.graphData.setData(index, section.getData());

        this.sectionCount++;
    }

    public void updateNode(RenderSection section, BuiltSectionInfo data) {
        var index = section.getLocalSectionIndex();

        if (this.sections[index] != section) {
            throw new RuntimeException("Section does not belong to this region");
        }

        this.graphData.setData(index, data);
    }

    public void removeChunk(RenderSection section) {
        int index = section.getLocalSectionIndex();

        if (this.sections[index] == null) {
            throw new RuntimeException("Tried to remove a chunk section that isn't present");
        }

        this.sectionStorage.forEach((pass, storage) -> {
            storage.deleteData(section.getLocalSectionIndex());
        });

        this.sections[index] = null;
        this.graphData.setData(index, null);

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
    public SectionRenderDataStorage getStorage(TerrainRenderPass pass) {
        return this.sectionStorage.get(pass);
    }

    public void cleanup(CommandList commandList) {
        if (this.resources != null && this.resources.isEmpty()) {
            this.resources.delete(commandList);
            this.resources = null;
        }
    }

    public RenderSection[] getChunks() {
        return this.sections;
    }

    public int getChunkX() {
        return this.x << REGION_WIDTH_SH;
    }

    public int getChunkY() {
        return this.y << REGION_HEIGHT_SH;
    }

    public int getChunkZ() {
        return this.z << REGION_LENGTH_SH;
    }

    public int getX() {
        return this.x;
    }

    public int getY() {
        return this.y;
    }

    public int getZ() {
        return this.z;
    }

    public long key() {
        return ChunkSectionPos.asLong(this.x, this.y, this.z);
    }

    public boolean testVisibility(Frustum frustum) {
        return frustum.testBox(this.getWorldX(), this.getWorldY(), this.getWorldZ(),
                this.getWorldX() + (RenderRegion.REGION_WIDTH << 4), this.getWorldY() + (RenderRegion.REGION_HEIGHT << 4), this.getWorldZ() + (RenderRegion.REGION_LENGTH << 4));
    }

    public void refreshPointers(CommandList commandList) {
        if (this.resources != null) {
            this.resources.deleteTessellation(commandList);
        }

        for (var storage : this.sectionStorage.values()) {
            storage.refreshPointers();
        }
    }

    public static class Resources {
        private final GlBufferArena vertexBuffers;
        private GlTessellation tessellation;

        public Resources(CommandList commandList, StagingBuffer stagingBuffer) {
            int stride = ChunkMeshFormats.COMPACT.getVertexFormat().getStride();

            this.vertexBuffers = new GlBufferArena(commandList, REGION_SIZE * 756, stride, stagingBuffer);
        }

        public void updateTessellation(CommandList commandList, GlTessellation tessellation) {
            if (this.tessellation != null) {
                this.tessellation.delete(commandList);
            }

            this.tessellation = tessellation;
        }

        public void deleteTessellation(CommandList commandList) {
            if (this.tessellation != null) {
                this.tessellation.delete(commandList);
            }

            this.tessellation = null;
        }

        public GlTessellation getTessellation() {
            return this.tessellation;
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
            this.deleteTessellation(commandList);

            this.vertexBuffers.delete(commandList);
        }

        public GlBuffer getVertexBuffer() {
            return this.vertexBuffers.getBufferObject();
        }

        public GlBufferArena getGeometryArena() {
            return this.vertexBuffers;
        }
    }
}
