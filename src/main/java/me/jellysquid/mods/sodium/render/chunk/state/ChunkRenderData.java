package me.jellysquid.mods.sodium.render.chunk.state;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import me.jellysquid.mods.sodium.render.chunk.passes.ChunkRenderPass;
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
    public static final ChunkRenderData ABSENT = new ChunkRenderData.Builder()
            .build();
    public static final ChunkRenderData EMPTY = createEmptyData();

    private List<BlockEntity> globalBlockEntities;
    private List<BlockEntity> blockEntities;

    private ChunkOcclusionData occlusionData;
    private ChunkRenderBounds bounds;

    private List<Sprite> animatedSprites;

    private boolean isEmpty;

    /**
     * @return True if the chunk has no renderables, otherwise false
     */
    public boolean isEmpty() {
        return this.isEmpty;
    }

    public ChunkRenderBounds getBounds() {
        return this.bounds;
    }

    public ChunkOcclusionData getOcclusionData() {
        return this.occlusionData;
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

    public static class Builder {
        private final List<BlockEntity> globalBlockEntities = new ArrayList<>();
        private final List<BlockEntity> blockEntities = new ArrayList<>();
        private final Set<Sprite> animatedSprites = new ObjectOpenHashSet<>();

        private ChunkOcclusionData occlusionData;
        private ChunkRenderBounds bounds = ChunkRenderBounds.ALWAYS_FALSE;
        private Set<ChunkRenderPass> nonEmptyMeshes = new ReferenceOpenHashSet<>();

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
            (cull ? this.blockEntities : this.globalBlockEntities).add(entity);
        }

        public void addNonEmptyMesh(ChunkRenderPass pass) {
            this.nonEmptyMeshes.add(pass);
        }

        public ChunkRenderData build() {
            ChunkRenderData data = new ChunkRenderData();
            data.globalBlockEntities = this.globalBlockEntities;
            data.blockEntities = this.blockEntities;
            data.occlusionData = this.occlusionData;
            data.bounds = this.bounds;
            data.animatedSprites = new ObjectArrayList<>(this.animatedSprites);
            data.isEmpty = this.nonEmptyMeshes.isEmpty() && this.globalBlockEntities.isEmpty() && this.blockEntities.isEmpty();

            return data;
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