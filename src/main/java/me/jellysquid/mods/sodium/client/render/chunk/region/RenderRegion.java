package me.jellysquid.mods.sodium.client.render.chunk.region;

import it.unimi.dsi.fastutil.objects.Reference2ReferenceOpenHashMap;
import me.jellysquid.mods.sodium.client.gl.arena.GlBufferArena;
import me.jellysquid.mods.sodium.client.gl.arena.staging.StagingBuffer;
import me.jellysquid.mods.sodium.client.gl.buffer.GlBuffer;
import me.jellysquid.mods.sodium.client.gl.device.CommandList;
import me.jellysquid.mods.sodium.client.gl.tessellation.GlTessellation;
import me.jellysquid.mods.sodium.client.render.chunk.RenderSection;
import me.jellysquid.mods.sodium.client.render.chunk.data.SectionRenderDataStorage;
import me.jellysquid.mods.sodium.client.render.chunk.gfni.TranslucentData;
import me.jellysquid.mods.sodium.client.render.chunk.lists.ChunkRenderList;
import me.jellysquid.mods.sodium.client.render.chunk.terrain.TerrainRenderPass;
import me.jellysquid.mods.sodium.client.render.chunk.vertex.format.ChunkMeshFormats;
import me.jellysquid.mods.sodium.client.util.MathUtil;
import net.minecraft.util.math.ChunkSectionPos;
import org.apache.commons.lang3.Validate;

import java.util.Arrays;
import java.util.Map;

public class RenderRegion {
    public static final int REGION_WIDTH = 8;
    public static final int REGION_HEIGHT = 4;
    public static final int REGION_LENGTH = 8;

    private static final int REGION_WIDTH_M = RenderRegion.REGION_WIDTH - 1;
    private static final int REGION_HEIGHT_M = RenderRegion.REGION_HEIGHT - 1;
    private static final int REGION_LENGTH_M = RenderRegion.REGION_LENGTH - 1;

    protected static final int REGION_WIDTH_SH = Integer.bitCount(REGION_WIDTH_M);
    protected static final int REGION_HEIGHT_SH = Integer.bitCount(REGION_HEIGHT_M);
    protected static final int REGION_LENGTH_SH = Integer.bitCount(REGION_LENGTH_M);

    public static final int REGION_SIZE = REGION_WIDTH * REGION_HEIGHT * REGION_LENGTH;

    static {
        Validate.isTrue(MathUtil.isPowerOfTwo(REGION_WIDTH));
        Validate.isTrue(MathUtil.isPowerOfTwo(REGION_HEIGHT));
        Validate.isTrue(MathUtil.isPowerOfTwo(REGION_LENGTH));
    }

    private final StagingBuffer stagingBuffer;
    private final int x, y, z;

    private final ChunkRenderList renderList;

    private final RenderSection[] sections = new RenderSection[RenderRegion.REGION_SIZE];
    private int sectionCount;

    private final Map<TerrainRenderPass, SectionRenderDataStorage> sectionRenderData = new Reference2ReferenceOpenHashMap<>();
    private SectionRenderDataStorage translucentIndexData = null;
    private DeviceResources resources;

    public RenderRegion(int x, int y, int z, StagingBuffer stagingBuffer) {
        this.x = x;
        this.y = y;
        this.z = z;

        this.stagingBuffer = stagingBuffer;
        this.renderList = new ChunkRenderList(this);
    }

    public static long key(int x, int y, int z) {
        return ChunkSectionPos.asLong(x, y, z);
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

    public int getOriginX() {
        return this.getChunkX() << 4;
    }

    public int getOriginY() {
        return this.getChunkY() << 4;
    }

    public int getOriginZ() {
        return this.getChunkZ() << 4;
    }

    public void delete(CommandList commandList) {
        for (var storage : this.sectionRenderData.values()) {
            storage.delete();
        }
        if (this.translucentIndexData != null) {
            this.translucentIndexData.delete();
        }

        this.sectionRenderData.clear();

        if (this.resources != null) {
            this.resources.delete(commandList);
            this.resources = null;
        }

        Arrays.fill(this.sections, null);
    }

    public boolean isEmpty() {
        return this.sectionCount == 0;
    }

    public SectionRenderDataStorage getStorage(TerrainRenderPass pass) {
        return this.sectionRenderData.get(pass);
    }

    public SectionRenderDataStorage getTranslucentStorage() {
        return this.translucentIndexData;
    }

    public SectionRenderDataStorage createStorage(TerrainRenderPass pass) {
        var storage = this.sectionRenderData.get(pass);

        if (storage == null) {
            this.sectionRenderData.put(pass, storage = new SectionRenderDataStorage());
        }

        return storage;
    }

    public SectionRenderDataStorage createTranslucentStorage() {
        if (this.translucentIndexData == null) {
            return this.translucentIndexData = new SectionRenderDataStorage();
        }

        return this.translucentIndexData;
    }

    public void refresh(CommandList commandList) {
        if (this.resources != null) {
            this.resources.deleteTessellations(commandList);
        }

        for (var storage : this.sectionRenderData.values()) {
            storage.onBufferResized();
        }
    }

    public void refreshTranslucent(CommandList commandList) {
        if (this.resources != null) {
            this.resources.deleteTessellations(commandList);
        }

        if (this.translucentIndexData != null) {
            this.translucentIndexData.onBufferResized();
        }
    }

    public void addSection(RenderSection section) {
        var sectionIndex = section.getSectionIndex();
        var prev = this.sections[sectionIndex];

        if (prev != null) {
            throw new IllegalStateException("Section has already been added to the region");
        }

        this.sections[sectionIndex] = section;
        this.sectionCount++;
    }

    public void removeSection(RenderSection section) {
        var sectionIndex = section.getSectionIndex();
        var prev = this.sections[sectionIndex];

        if (prev == null) {
            throw new IllegalStateException("Section was not loaded within the region");
        } else if (prev != section) {
            throw new IllegalStateException("Tried to remove the wrong section");
        }

        for (var storage : this.sectionRenderData.values()) {
            storage.removeMeshes(sectionIndex);
        }
        if (this.translucentIndexData != null) {
            this.translucentIndexData.removeMeshes(sectionIndex);
        }

        this.sections[sectionIndex] = null;
        this.sectionCount--;
    }

    public RenderSection getSection(int id) {
        return this.sections[id];
    }

    public DeviceResources getResources() {
        return this.resources;
    }

    public DeviceResources createResources(CommandList commandList) {
        if (this.resources == null) {
            this.resources = new DeviceResources(commandList, this.stagingBuffer);
        }

        return this.resources;
    }

    public void update(CommandList commandList) {
        if (this.resources != null && this.resources.shouldDelete()) {
            this.resources.delete(commandList);
            this.resources = null;
        }
    }

    public ChunkRenderList getRenderList() {
        return this.renderList;
    }

    public static class DeviceResources {
        private final GlBufferArena geometryArena;
        private final GlBufferArena indexArena;
        private GlTessellation tessellation;
        private GlTessellation indexedTessellation;

        public DeviceResources(CommandList commandList, StagingBuffer stagingBuffer) {
            int stride = ChunkMeshFormats.COMPACT.getVertexFormat().getStride();
            this.geometryArena = new GlBufferArena(commandList, REGION_SIZE * 756, stride, stagingBuffer);

            // TODO: what size should this be intially? Should this be using the same staging buffer?
            this.indexArena = new GlBufferArena(commandList, REGION_SIZE * 756, TranslucentData.BYTES_PER_INDEX, stagingBuffer);
        }

        public void updateTessellation(CommandList commandList, GlTessellation tessellation) {
            if (this.tessellation != null) {
                this.tessellation.delete(commandList);
            }

            this.tessellation = tessellation;
        }

        public void updateIndexedTessellation(CommandList commandList, GlTessellation tessellation) {
            if (this.indexedTessellation != null) {
                this.indexedTessellation.delete(commandList);
            }

            this.indexedTessellation = tessellation;
        }

        public GlTessellation getTessellation() {
            return this.tessellation;
        }

        public GlTessellation getIndexedTessellation() {
            return this.indexedTessellation;
        }

        public void deleteTessellations(CommandList commandList) {
            if (this.tessellation != null) {
                this.tessellation.delete(commandList);
                this.tessellation = null;
            }
            if (this.indexedTessellation != null) {
                this.indexedTessellation.delete(commandList);
                this.indexedTessellation = null;
            }
        }

        public GlBuffer getVertexBuffer() {
            return this.geometryArena.getBufferObject();
        }

        public GlBuffer getIndexBuffer() {
            return this.indexArena.getBufferObject();
        }

        public void delete(CommandList commandList) {
            this.deleteTessellations(commandList);
            this.geometryArena.delete(commandList);
            this.indexArena.delete(commandList);
        }

        public GlBufferArena getGeometryArena() {
            return this.geometryArena;
        }

        public GlBufferArena getIndexArena() {
            return this.indexArena;
        }

        public boolean shouldDelete() {
            return this.geometryArena.isEmpty() && this.indexArena.isEmpty(); // TODO: correct?
        }
    }
}
