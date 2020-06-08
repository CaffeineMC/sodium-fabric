package me.jellysquid.mods.sodium.client.render.chunk;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.render.chunk.ChunkOcclusionData;
import net.minecraft.client.texture.Sprite;
import net.minecraft.util.math.Direction;

import java.util.*;

/**
 * The render data for a chunk render container containing all the information about which meshes are attached, the
 * block entities contained by it, and any data used for occlusion testing.
 */
public class ChunkRenderData {
    public static final ChunkRenderData ABSENT = new ChunkRenderData.Builder().build();
    public static final ChunkRenderData EMPTY = createEmptyData();

    private final List<BlockEntity> globalBlockEntities;
    private final List<BlockEntity> blockEntities;
    private final List<Sprite> animatedSprites;

    private final ChunkMeshData meshData;

    private final ChunkOcclusionData occlusionData;
    private final boolean isEmpty;

    public ChunkRenderData(List<BlockEntity> globalBlockEntities, List<BlockEntity> blockEntities, List<Sprite> animatedSprites, ChunkOcclusionData occlusionData, ChunkMeshData meshData) {
        this.globalBlockEntities = globalBlockEntities;
        this.blockEntities = blockEntities;
        this.animatedSprites = animatedSprites;
        this.occlusionData = occlusionData;
        this.meshData = meshData;

        this.isEmpty = this.globalBlockEntities.isEmpty() && this.blockEntities.isEmpty() && this.meshData.isEmpty();
    }

    /**
     * @return True if the chunk has no renderables, otherwise false
     */
    public boolean isEmpty() {
        return this.isEmpty;
    }

    /**
     * @param from The direction from which this node is being traversed through on the graph
     * @param to The direction from this node into the adjacent to be tested
     * @return True if this chunk can cull the neighbor given the incoming direction
     */
    public boolean isVisibleThrough(Direction from, Direction to) {
        return this.occlusionData != null && this.occlusionData.isVisibleThrough(from, to);
    }

    public List<Sprite> getAnimatedSprites() {
        return this.animatedSprites;
    }

    /**
     * The collection of block entities contained by this rendered chunk.
     */
    public Collection<BlockEntity> getBlockEntities() {
        return this.blockEntities;
    }

    /**
     * The collection of block entities contained by this rendered chunk section which are not part of its culling
     * volume. These entities should always be rendered regardless of the render being visible in the frustum.
     */
    public Collection<BlockEntity> getGlobalBlockEntities() {
        return this.globalBlockEntities;
    }

    /**
     * The collection of chunk meshes belonging to this render.
     */
    public ChunkMeshData getMeshData() {
        return this.meshData;
    }

    public static class Builder {
        private final List<BlockEntity> globalEntities = new ArrayList<>();
        private final List<BlockEntity> blockEntities = new ArrayList<>();
        private final Set<Sprite> animatedSprites = new ObjectOpenHashSet<>();

        private ChunkMeshData meshData = ChunkMeshData.EMPTY;
        private ChunkOcclusionData occlusionData;

        public void setOcclusionData(ChunkOcclusionData data) {
            this.occlusionData = data;
        }

        /**
         * Adds the sprites to this data container for tracking. See {@link Builder#addSprite(Sprite)}.
         * @param sprites The collection of sprites to be added
         */
        public void addSprites(Sprite[] sprites) {
            for (Sprite sprite : sprites) {
                this.addSprite(sprite);
            }
        }

        /**
         * Adds a sprite to this data container for tracking. If the sprite is tickable, it will be ticked every frame
         * before rendering as necessary.
         * @param sprite The sprite
         */
        public void addSprite(Sprite sprite) {
            if (sprite.isAnimated()) {
                this.animatedSprites.add(sprite);
            }
        }

        public void setMeshData(ChunkMeshData data) {
            this.meshData = data;
        }

        /**
         * Adds a block entity to the data container.
         * @param entity The block entity itself
         * @param cull True if the block entity can be culled to this chunk render's volume, otherwise false
         */
        public void addBlockEntity(BlockEntity entity, boolean cull) {
            (cull ? this.blockEntities : this.globalEntities).add(entity);
        }

        public ChunkRenderData build() {
            return new ChunkRenderData(this.globalEntities, this.blockEntities, new ObjectArrayList<>(this.animatedSprites), this.occlusionData, this.meshData);
        }
    }

    private static ChunkRenderData createEmptyData() {
        ChunkOcclusionData occlusionData = new ChunkOcclusionData();
        occlusionData.addOpenEdgeFaces(EnumSet.allOf(Direction.class));

        ChunkRenderData.Builder meshInfo = new ChunkRenderData.Builder();
        meshInfo.setOcclusionData(occlusionData);
        meshInfo.setMeshData(ChunkMeshData.EMPTY);

        return meshInfo.build();
    }
}