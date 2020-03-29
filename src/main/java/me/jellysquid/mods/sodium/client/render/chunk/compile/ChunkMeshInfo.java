package me.jellysquid.mods.sodium.client.render.chunk.compile;

import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import me.jellysquid.mods.sodium.client.render.vertex.BufferUploadData;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.chunk.ChunkOcclusionData;
import net.minecraft.util.math.Direction;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class ChunkMeshInfo {
    public static final ChunkMeshInfo ABSENT = new ChunkMeshInfo.Builder().build();

    private final List<BlockEntity> globalBlockEntities;
    private final List<BlockEntity> blockEntities;

    private final Object2ObjectMap<RenderLayer, LayerMeshInfo> layers;

    private final ChunkOcclusionData occlusionData;

    public ChunkMeshInfo(List<BlockEntity> globalBlockEntities, List<BlockEntity> blockEntities, ChunkOcclusionData occlusionData, Object2ObjectMap<RenderLayer, LayerMeshInfo> layers) {
        this.globalBlockEntities = globalBlockEntities;
        this.blockEntities = blockEntities;
        this.occlusionData = occlusionData;
        this.layers = layers;
    }

    public boolean isEmpty() {
        return this.layers.isEmpty();
    }

    public boolean isVisibleThrough(Direction from, Direction to) {
        return this.occlusionData != null && this.occlusionData.isVisibleThrough(from, to);
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

        private final Object2ObjectMap<RenderLayer, LayerMeshInfo> layers = new Object2ObjectArrayMap<>(4);

        private ChunkOcclusionData occlusionData;

        public void setOcclusionData(ChunkOcclusionData data) {
            this.occlusionData = data;
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
            return new ChunkMeshInfo(this.globalEntities, this.blockEntities, this.occlusionData, this.layers);
        }
    }
}