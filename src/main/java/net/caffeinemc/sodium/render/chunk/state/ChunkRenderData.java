package net.caffeinemc.sodium.render.chunk.state;

import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import net.caffeinemc.sodium.render.chunk.passes.ChunkRenderPass;
import net.caffeinemc.sodium.util.DirectionUtil;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.render.chunk.ChunkOcclusionData;
import net.minecraft.client.texture.Sprite;
import net.minecraft.util.math.Direction;

import java.util.*;
import java.util.function.IntFunction;

/**
 * The render data for a chunk render container containing all the information about which meshes are attached, the
 * block entities contained by it, and any data used for occlusion testing.
 */
public class ChunkRenderData {
    public static final ChunkRenderData ABSENT = new ChunkRenderData.Builder()
            .build();
    public static final ChunkRenderData EMPTY = createEmptyData();

    public final BlockEntity[] globalBlockEntities;
    public final BlockEntity[] blockEntities;
    public final Sprite[] animatedSprites;
    public final ChunkRenderPass[] meshes;
    public final ChunkRenderBounds bounds;
    public final long visibilityData;

    public ChunkRenderData(BlockEntity[] globalBlockEntities, BlockEntity[] blockEntities, Sprite[] animatedSprites,
                           ChunkRenderPass[] meshes, ChunkRenderBounds bounds, ChunkOcclusionData occlusionData) {
        this.globalBlockEntities = globalBlockEntities;
        this.blockEntities = blockEntities;
        this.animatedSprites = animatedSprites;
        this.meshes = meshes;
        this.bounds = bounds;
        this.visibilityData = calculateVisibilityData(occlusionData);
    }

    public static class Builder {
        private final List<BlockEntity> globalBlockEntities = new ArrayList<>();
        private final List<BlockEntity> localBlockEntities = new ArrayList<>();
        private final Set<Sprite> animatedSprites = new ObjectOpenHashSet<>();
        private final Set<ChunkRenderPass> meshes = new ReferenceOpenHashSet<>();

        private ChunkOcclusionData occlusionData;
        private ChunkRenderBounds bounds = ChunkRenderBounds.ALWAYS_FALSE;

        public void setBounds(ChunkRenderBounds bounds) {
            this.bounds = bounds;
        }

        public void setOcclusionData(ChunkOcclusionData data) {
            this.occlusionData = data;
        }

        /**
         * Adds a sprite to this data container for tracking. If the sprite is tickable, it will be ticked every frame
         * before rendering as necessary.
         * @param sprite The sprite
         */
        public void addSprite(Sprite sprite) {
            if (sprite.getAnimation() != null) {
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

        public void addMesh(ChunkRenderPass pass) {
            this.meshes.add(pass);
        }

        public ChunkRenderData build() {
            var globalBlockEntities = toArray(this.globalBlockEntities, BlockEntity[]::new);
            var blockEntities = toArray(this.localBlockEntities, BlockEntity[]::new);
            var animatedSprites = toArray(this.animatedSprites, Sprite[]::new);
            var meshes = toArray(this.meshes, ChunkRenderPass[]::new);

            return new ChunkRenderData(globalBlockEntities, blockEntities, animatedSprites, meshes, this.bounds, this.occlusionData);
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

    private static long calculateVisibilityData(ChunkOcclusionData occlusionData) {
        long bits = 0L;

        for (var from : DirectionUtil.ALL_DIRECTIONS) {
            for (var to : DirectionUtil.ALL_DIRECTIONS) {
                if (occlusionData == null || occlusionData.isVisibleThrough(from, to)) {
                    bits |= (1L << ((from.ordinal() << 3) + to.ordinal()));
                }
            }
        }

        return bits;
    }
}