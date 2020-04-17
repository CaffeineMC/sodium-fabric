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

public class ChunkRenderData {
    public static final ChunkRenderData ABSENT = new ChunkRenderData.Builder().build();
    public static final ChunkRenderData EMPTY = createEmptyData();

    private final List<BlockEntity> globalBlockEntities;
    private final List<BlockEntity> blockEntities;
    private final List<Sprite> animatedSprites;

    private final Object2ObjectMap<BlockRenderPass, ChunkMesh> meshes;

    private final ChunkOcclusionData occlusionData;

    public ChunkRenderData(List<BlockEntity> globalBlockEntities, List<BlockEntity> blockEntities, List<Sprite> animatedSprites, ChunkOcclusionData occlusionData, Object2ObjectMap<BlockRenderPass, ChunkMesh> meshes) {
        this.globalBlockEntities = globalBlockEntities;
        this.blockEntities = blockEntities;
        this.animatedSprites = animatedSprites;
        this.occlusionData = occlusionData;
        this.meshes = meshes;
    }

    public boolean isEmpty() {
        return this.meshes.isEmpty();
    }

    public boolean isVisibleThrough(Direction from, Direction to) {
        return this.occlusionData != null && this.occlusionData.isVisibleThrough(from, to);
    }

    public List<Sprite> getAnimatedSprites() {
        return this.animatedSprites;
    }

    public Collection<BlockEntity> getBlockEntities() {
        return this.blockEntities;
    }

    public Collection<BlockEntity> getGlobalBlockEntities() {
        return this.globalBlockEntities;
    }

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

        public void addSprite(Sprite sprite) {
            if (sprite.isAnimated()) {
                this.animatedSprites.add(sprite);
            }
        }

        public void addMeshes(List<ChunkMesh> meshes) {
            for (ChunkMesh mesh : meshes) {
                if (this.meshes.putIfAbsent(mesh.getRenderPass(), mesh) != null) {
                    throw new IllegalArgumentException("Mesh already added");
                }
            }
        }

        public void addBlockEntity(BlockEntity entity, boolean cull) {
            (cull ? this.blockEntities : this.globalEntities).add(entity);
        }

        public ChunkRenderData build() {
            return new ChunkRenderData(this.globalEntities, this.blockEntities, new ObjectArrayList<>(this.animatedSprites), this.occlusionData, this.meshes);
        }

        public void addSprites(Sprite[] sprites) {
            for (Sprite sprite : sprites) {
                this.addSprite(sprite);
            }
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