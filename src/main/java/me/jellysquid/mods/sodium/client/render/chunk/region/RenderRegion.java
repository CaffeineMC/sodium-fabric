package me.jellysquid.mods.sodium.client.render.chunk.region;

import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import me.jellysquid.mods.sodium.client.gl.arena.GlBufferArena;
import me.jellysquid.mods.sodium.client.gl.device.CommandList;
import me.jellysquid.mods.sodium.client.gl.tessellation.GlTessellation;
import me.jellysquid.mods.sodium.client.render.chunk.ChunkRenderer;
import me.jellysquid.mods.sodium.client.render.chunk.RenderSection;
import me.jellysquid.mods.sodium.client.render.chunk.passes.BlockRenderPass;
import me.jellysquid.mods.sodium.client.util.MathUtil;
import me.jellysquid.mods.sodium.client.util.math.FrustumExtended;
import net.minecraft.util.math.ChunkSectionPos;
import org.apache.commons.lang3.Validate;

import java.util.EnumMap;
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

    static {
        Validate.isTrue(MathUtil.isPowerOfTwo(REGION_WIDTH));
        Validate.isTrue(MathUtil.isPowerOfTwo(REGION_HEIGHT));
        Validate.isTrue(MathUtil.isPowerOfTwo(REGION_LENGTH));
    }

    private final ChunkRenderer renderer;

    private final Set<RenderSection> chunks = new ObjectOpenHashSet<>();
    private final Map<BlockRenderPass, RenderRegionArenas> arenas = new EnumMap<>(BlockRenderPass.class);

    private final int x, y, z;

    private RenderRegionVisibility visibility;

    public RenderRegion(ChunkRenderer renderer, int x, int y, int z) {
        this.renderer = renderer;

        this.x = x;
        this.y = y;
        this.z = z;
    }

    public static RenderRegion createRegionForChunk(ChunkRenderer renderer, int x, int y, int z) {
        return new RenderRegion(renderer, x >> REGION_WIDTH_SH, y >> REGION_HEIGHT_SH, z >> REGION_LENGTH_SH);
    }

    public RenderRegionArenas getArenas(BlockRenderPass pass) {
        return this.arenas.get(pass);
    }

    public void deleteResources(CommandList commandList) {
        for (RenderRegionArenas arenas : this.arenas.values()) {
            arenas.delete(commandList);
        }

        this.arenas.clear();
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

    public RenderRegionArenas getOrCreateArenas(CommandList commandList, BlockRenderPass pass) {
        RenderRegionArenas arenas = this.arenas.get(pass);

        if (arenas == null) {
            this.arenas.put(pass, arenas = new RenderRegionArenas(commandList, this.renderer));
        }

        return arenas;
    }

    public void addChunk(RenderSection chunk) {
        if (!this.chunks.add(chunk)) {
            throw new IllegalStateException("Chunk " + chunk + " is already a member of region " + this);
        }
    }

    public void removeChunk(RenderSection chunk) {
        if (!this.chunks.remove(chunk)) {
            throw new IllegalStateException("Chunk " + chunk + " is not a member of region " + this);
        }
    }

    public boolean isEmpty() {
        return this.chunks.isEmpty();
    }

    public int getChunkCount() {
        return this.chunks.size();
    }

    public void updateVisibility(FrustumExtended frustum) {
        int x = this.getOriginX();
        int y = this.getOriginY();
        int z = this.getOriginZ();

        this.visibility = frustum.aabbTest(x, y, z,
                x + (REGION_WIDTH << 4), y + (REGION_HEIGHT << 4), z + (REGION_LENGTH << 4));
    }

    public RenderRegionVisibility getVisibility() {
        return this.visibility;
    }

    public static int getChunkIndex(int x, int y, int z) {
        return (x * RenderRegion.REGION_LENGTH * RenderRegion.REGION_HEIGHT) + (y * RenderRegion.REGION_LENGTH) + z;
    }

    public static class RenderRegionArenas {
        public final GlBufferArena vertexBuffers;
        public final GlBufferArena indexBuffers;

        public GlTessellation tessellation;

        public RenderRegionArenas(CommandList commandList, ChunkRenderer renderer) {
            this.vertexBuffers = new GlBufferArena(commandList, 24 * 1024, renderer.getVertexType().getBufferVertexFormat().getStride());
            this.indexBuffers = new GlBufferArena(commandList, 6 * 1024, 4);
        }

        public void delete(CommandList commandList) {
            this.vertexBuffers.delete(commandList);
            this.indexBuffers.delete(commandList);

            if (this.tessellation != null) {
                commandList.deleteTessellation(this.tessellation);
            }
        }

        public void setTessellation(GlTessellation tessellation) {
            this.tessellation = tessellation;
        }

        public GlTessellation getTessellation() {
            return this.tessellation;
        }

        public boolean isEmpty() {
            return this.vertexBuffers.isEmpty() && this.indexBuffers.isEmpty();
        }

        public long getUsedMemory() {
            return this.vertexBuffers.getUsedMemory() + this.indexBuffers.getUsedMemory();
        }

        public long getAllocatedMemory() {
            return this.vertexBuffers.getAllocatedMemory() + this.indexBuffers.getAllocatedMemory();
        }

        public void invalidateTessellation(CommandList commandList) {
            if (this.tessellation != null) {
                commandList.deleteTessellation(this.tessellation);

                this.tessellation = null;
            }
        }
    }
}
