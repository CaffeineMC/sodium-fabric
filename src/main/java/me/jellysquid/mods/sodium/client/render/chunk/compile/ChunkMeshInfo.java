package me.jellysquid.mods.sodium.client.render.chunk.compile;

import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import me.jellysquid.mods.sodium.client.render.vertex.BufferUploadData;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.chunk.ChunkOcclusionData;
import net.minecraft.client.texture.Sprite;
import net.minecraft.util.math.Direction;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

public class ChunkMeshInfo {
    public static final ChunkMeshInfo ABSENT = new ChunkMeshInfo.Builder().build();

    private final List<BlockEntity> globalBlockEntities;
    private final List<BlockEntity> blockEntities;
    private final List<Sprite> animatedSprites;

    private final Object2ObjectMap<RenderLayer, LayerMeshInfo> layers;

    private final ChunkOcclusionData occlusionData;

    public ChunkMeshInfo(List<BlockEntity> globalBlockEntities, List<BlockEntity> blockEntities, List<Sprite> animatedSprites, ChunkOcclusionData occlusionData, Object2ObjectMap<RenderLayer, LayerMeshInfo> layers) {
        this.globalBlockEntities = globalBlockEntities;
        this.blockEntities = blockEntities;
        this.animatedSprites = animatedSprites;
        this.occlusionData = occlusionData;
        this.layers = layers;
    }

    public boolean isEmpty() {
        return this.layers.isEmpty();
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

    public Iterable<LayerMeshInfo> getLayers() {
        return this.layers.values();
    }

    public static class Builder {
        private final List<BlockEntity> globalEntities = new ArrayList<>();
        private final List<BlockEntity> blockEntities = new ArrayList<>();
        private final Set<Sprite> animatedSprites = new ObjectOpenHashSet<>();

        private final Object2ObjectMap<RenderLayer, LayerMeshInfo> layers = new Object2ObjectArrayMap<>(4);

        private ChunkOcclusionData occlusionData;

        public void setOcclusionData(ChunkOcclusionData data) {
            this.occlusionData = data;
        }

        public void addSprite(Sprite sprite) {
            if (sprite.isAnimated()) {
                this.animatedSprites.add(sprite);
            }
        }

        public void addMeshData(RenderLayer layer, BufferUploadData data) {
            if (this.layers.putIfAbsent(layer, new LayerMeshInfo(layer, data)) != null) {
                throw new IllegalArgumentException("Mesh already added");
            }
        }

        public void addBlockEntity(BlockEntity entity, boolean cull) {
            (cull ? this.blockEntities : this.globalEntities).add(entity);
        }

        public ChunkMeshInfo build() {
            return new ChunkMeshInfo(this.globalEntities, this.blockEntities, new ObjectArrayList<>(this.animatedSprites), this.occlusionData, this.layers);
        }

        public void addSprites(Sprite[] sprites) {
            for (Sprite sprite : sprites) {
                this.addSprite(sprite);
            }
        }
    }
}