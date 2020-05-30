package me.jellysquid.mods.sodium.client.render.chunk;

import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import me.jellysquid.mods.sodium.client.render.layer.BlockRenderPass;
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

    private final Object2ObjectMap<BlockRenderPass, ChunkMesh> meshes;

    private final ChunkOcclusionData occlusionData;
    private final boolean isEmpty;

    public ChunkRenderData(List<BlockEntity> globalBlockEntities, List<BlockEntity> blockEntities, List<Sprite> animatedSprites, ChunkOcclusionData occlusionData, Object2ObjectMap<BlockRenderPass, ChunkMesh> meshes) {
        this.globalBlockEntities = globalBlockEntities;
        this.blockEntities = blockEntities;
        this.animatedSprites = animatedSprites;
        this.occlusionData = occlusionData;
        this.meshes = meshes;

        this.isEmpty = this.globalBlockEntities.isEmpty() && this.blockEntities.isEmpty() && this.meshes.isEmpty();
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
    public Collection<ChunkMesh> getMeshes() {
        return this.meshes.values();
    }

    public static class Builder {
        private final List<BlockEntity> globalEntities = new ArrayList<>();
        private final List<BlockEntity> blockEntities = new ArrayList<>();
        private final Set<Sprite> animatedSprites = new ObjectOpenHashSet<>();

        private final Object2ObjectMap<BlockRenderPass, ChunkMesh> meshes = new Object2ObjectArrayMap<>(4);

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

        /**
         * Adds the collection of meshes to this data container. See {@link Builder#addMesh(ChunkMesh)}.
         * @param meshes The collection of non-null meshes
         */
        public void addMeshes(List<ChunkMesh> meshes) {
            for (ChunkMesh mesh : meshes) {
                this.addMesh(mesh);
            }
        }

        /**
         * Adds the given mesh to this data container for use in uploads and rendering. A chunk data container can only
         * contain one mesh for a given render pass.
         * @throws IllegalArgumentException If a mesh is already attached for the given render pass
         * @param mesh The chunk mesh to add to this container
         */
        public void addMesh(ChunkMesh mesh) {
            if (this.meshes.putIfAbsent(mesh.getRenderPass(), mesh) != null) {
                throw new IllegalArgumentException("Mesh already added");
            }
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
            return new ChunkRenderData(this.globalEntities, this.blockEntities, new ObjectArrayList<>(this.animatedSprites), this.occlusionData, this.meshes);
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