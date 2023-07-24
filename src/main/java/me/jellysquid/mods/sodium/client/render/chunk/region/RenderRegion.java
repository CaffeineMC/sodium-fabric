package me.jellysquid.mods.sodium.client.render.chunk.region;

import it.unimi.dsi.fastutil.objects.Reference2ReferenceOpenHashMap;
import me.jellysquid.mods.sodium.client.gl.arena.GlBufferArena;
import me.jellysquid.mods.sodium.client.gl.arena.staging.StagingBuffer;
import me.jellysquid.mods.sodium.client.gl.buffer.GlBuffer;
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

import java.util.Arrays;
import java.util.EnumMap;
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

    private final RenderSection[] sections = new RenderSection[RenderRegion.REGION_SIZE];
    private int sectionCount;

    private final Map<TerrainRenderPass, SectionStorage> sectionRenderData = new Reference2ReferenceOpenHashMap<>();
    private DeviceResources resources;

    public RenderRegion(int x, int y, int z, StagingBuffer stagingBuffer) {
        this.x = x;
        this.y = y;
        this.z = z;

        this.stagingBuffer = stagingBuffer;
    }

    public static long key(int x, int y, int z) {
        return ChunkSectionPos.asLong(x, y, z);
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

    public void delete(CommandList commandList) {
        for (var storage : this.sectionRenderData.values()) {
            storage.delete();
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

    public SectionStorage getStorage(TerrainRenderPass pass) {
        return this.sectionRenderData.get(pass);
    }

    public SectionStorage createStorage(TerrainRenderPass pass) {
        SectionStorage storage = this.sectionRenderData.get(pass);

        if (storage == null) {
            this.sectionRenderData.put(pass, storage = new SectionStorage());
        }

        return storage;
    }

    public void refresh(CommandList commandList) {
        if (this.resources != null) {
            this.resources.deleteTessellations(commandList);
        }

        for (var storage : this.sectionRenderData.values()) {
            storage.refresh();
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
            storage.updateState(section, null);
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

    public static class DeviceResources {
        private final EnumMap<SharedQuadIndexBuffer.IndexType, GlTessellation> tessellations = new EnumMap<>(SharedQuadIndexBuffer.IndexType.class);
        private final GlBufferArena geometryArena;

        public DeviceResources(CommandList commandList, StagingBuffer stagingBuffer) {
            int stride = ChunkMeshFormats.COMPACT.getVertexFormat().getStride();
            this.geometryArena = new GlBufferArena(commandList, REGION_SIZE * 756, stride, stagingBuffer);
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
                commandList.deleteTessellation(tessellation);
            }

            this.tessellations.clear();
        }

        public GlBuffer getVertexBuffer() {
            return this.geometryArena.getBufferObject();
        }

        public void delete(CommandList commandList) {
            this.deleteTessellations(commandList);
            this.geometryArena.delete(commandList);
        }

        public GlBufferArena getGeometryArena() {
            return this.geometryArena;
        }

        public boolean shouldDelete() {
            return this.geometryArena.isEmpty();
        }
    }

    public static class SectionStorage {
        private final ChunkGraphicsState[] state = new ChunkGraphicsState[RenderRegion.REGION_SIZE];

        public ChunkGraphicsState getRenderData(int section) {
            return this.state[section];
        }

        public void updateState(RenderSection section, ChunkGraphicsState state) {
            var id = section.getSectionIndex();

            var prev = this.state[id];

            if (prev != null) {
                prev.delete();
            }

            this.state[id] = state;
        }

        public void delete() {
            for (ChunkGraphicsState state : this.state) {
                if (state != null) {
                    state.delete();
                }
            }

            Arrays.fill(this.state, null);
        }


        public void refresh() {
            for (var state : this.state) {
                if (state != null) {
                    state.refresh();
                }
            }
        }
    }
}
