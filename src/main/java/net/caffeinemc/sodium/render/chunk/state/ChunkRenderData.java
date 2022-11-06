package net.caffeinemc.sodium.render.chunk.state;

import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.function.IntFunction;

import net.caffeinemc.sodium.render.texture.SpriteAnimationInterface;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.render.chunk.ChunkOcclusionData;
import net.minecraft.client.texture.Sprite;
import net.minecraft.util.math.Direction;

/**
 * The render data for a chunk render container containing all the information about which terrain models are attached,
 * the block entities contained by it, and any data used for occlusion testing.
 */
public class ChunkRenderData {
    public static final ChunkRenderData ABSENT = new ChunkRenderData.Builder()
            .build();
    public static final ChunkRenderData EMPTY = createEmptyData();

    public final BlockEntity[] globalBlockEntities;
    public final BlockEntity[] blockEntities;
    public final Sprite[] animatedSprites;
    public final ChunkPassModel[] models;
    public final ChunkRenderBounds bounds;
    public final ChunkOcclusionData occlusionData;

    public ChunkRenderData(BlockEntity[] globalBlockEntities, BlockEntity[] blockEntities, Sprite[] animatedSprites,
                           ChunkPassModel[] models, ChunkRenderBounds bounds, ChunkOcclusionData occlusionData) {
        this.globalBlockEntities = globalBlockEntities;
        this.blockEntities = blockEntities;
        this.animatedSprites = animatedSprites;
        this.models = models;
        this.bounds = bounds;
        this.occlusionData = occlusionData;
    }

    public int getFlags() {
        int flags = 0;

        if (this.globalBlockEntities != null) {
            flags |= ChunkRenderFlag.HAS_GLOBAL_BLOCK_ENTITIES;
        }

        if (this.blockEntities != null) {
            flags |= ChunkRenderFlag.HAS_BLOCK_ENTITIES;
        }

        if (this.models != null) {
            flags |= ChunkRenderFlag.HAS_TERRAIN_MODELS;
        }

        if (this.animatedSprites != null) {
            flags |= ChunkRenderFlag.HAS_TICKING_TEXTURES;
        }

        return flags;
    }

    public static class Builder {
        private final List<BlockEntity> globalBlockEntities = new ArrayList<>();
        private final List<BlockEntity> localBlockEntities = new ArrayList<>();
        private final Set<Sprite> animatedSprites = new ObjectOpenHashSet<>();
        
        private ChunkPassModel[] models;
        private ChunkOcclusionData occlusionData;
        private ChunkRenderBounds bounds = ChunkRenderBounds.ALWAYS_FALSE;

        public void setBounds(ChunkRenderBounds bounds) {
            this.bounds = bounds;
        }

        public void setOcclusionData(ChunkOcclusionData data) {
            this.occlusionData = data;
        }
        
        public void setModels(ChunkPassModel[] models) {
            this.models = models;
        }

        /**
         * Adds a sprite to this data container for tracking. If the sprite is tickable, it will be ticked every frame
         * before rendering as necessary.
         * @param sprite The sprite
         */
        public void addSprite(Sprite sprite) {
            if (((SpriteAnimationInterface) sprite.getContents()).hasAnimation()) {
                this.animatedSprites.add(sprite);
            }
        }

        /**
         * Adds a block entity to the data container.
         * @param entity The block entity itself
         * @param cull True if the block entity can be culled to this chunk render's volume, otherwise false
         */
        public void addBlockEntity(BlockEntity entity, boolean cull) {
            (cull ? this.localBlockEntities : this.globalBlockEntities).add(entity);
        }

        public ChunkRenderData build() {
            var globalBlockEntities = toArray(this.globalBlockEntities, BlockEntity[]::new);
            var blockEntities = toArray(this.localBlockEntities, BlockEntity[]::new);
            var animatedSprites = toArray(this.animatedSprites, Sprite[]::new);

            return new ChunkRenderData(
                    globalBlockEntities,
                    blockEntities,
                    animatedSprites,
                    this.models,
                    this.bounds,
                    this.occlusionData
            );
        }

        private static <T> T[] toArray(Collection<T> collection, IntFunction<T[]> factory) {
            if (collection.isEmpty()) {
                return null;
            }

            return collection.toArray(factory);
        }
    }

    private static ChunkRenderData createEmptyData() {
        ChunkOcclusionData occlusionData = new ChunkOcclusionData();
        occlusionData.addOpenEdgeFaces(EnumSet.allOf(Direction.class));

        ChunkRenderData.Builder meshInfo = new ChunkRenderData.Builder();
        meshInfo.setOcclusionData(occlusionData);

        return meshInfo.build();
    }
}