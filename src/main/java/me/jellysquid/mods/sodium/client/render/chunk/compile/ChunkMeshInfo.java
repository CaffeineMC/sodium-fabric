package me.jellysquid.mods.sodium.client.render.chunk.compile;

import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import me.jellysquid.mods.sodium.client.render.vertex.BufferUploadData;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.chunk.ChunkOcclusionData;
import net.minecraft.util.math.Direction;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ChunkMeshInfo {
    public static final ChunkMeshInfo ABSENT = new ChunkMeshInfo.Builder().build();

    private final List<BlockEntity> globalEntities;
    private final List<BlockEntity> blockEntities;

    private final ChunkOcclusionData occlusionData;
    private final List<RenderLayer> presentLayers;

    private List<MeshUpload> uploads;

    public ChunkMeshInfo(List<BlockEntity> globalEntities, List<BlockEntity> blockEntities, ChunkOcclusionData occlusionData, List<RenderLayer> presentLayers, List<MeshUpload> uploads) {
        this.globalEntities = globalEntities;
        this.blockEntities = blockEntities;
        this.occlusionData = occlusionData;
        this.presentLayers = presentLayers;

        this.uploads = uploads;
    }

    public boolean isEmpty() {
        return this.presentLayers.isEmpty();
    }

    public boolean containsLayer(RenderLayer layer) {
        return this.presentLayers.contains(layer);
    }

    public boolean isVisibleThrough(Direction from, Direction to) {
        return this.occlusionData != null && this.occlusionData.isVisibleThrough(from, to);
    }

    public Iterable<MeshUpload> getUploads() {
        return this.uploads;
    }

    public void clearUploads() {
        this.uploads = Collections.emptyList();
    }

    public static class Builder {
        private final List<BlockEntity> globalEntities = new ArrayList<>();
        private final List<BlockEntity> blockEntities = new ArrayList<>();

        private final Object2ObjectMap<RenderLayer, MeshUpload> layerData = new Object2ObjectArrayMap<>(4);

        private ChunkOcclusionData occlusionData;

        public void setOcclusionData(ChunkOcclusionData data) {
            this.occlusionData = data;
        }

        public void addMeshData(RenderLayer layer, BufferUploadData data) {
            if (this.layerData.putIfAbsent(layer, new MeshUpload(layer, data)) != null) {
                throw new IllegalArgumentException("Mesh already added");
            }
        }

        public void addBlockEntity(BlockEntity entity, boolean canCull) {
            (canCull ? this.blockEntities : this.globalEntities).add(entity);
        }

        public ChunkMeshInfo build() {
            return new ChunkMeshInfo(this.globalEntities, this.blockEntities, this.occlusionData,
                    new ObjectArrayList<>(this.layerData.keySet()), new ObjectArrayList<>(this.layerData.values()));
        }
    }

    public static class MeshUpload {
        public final RenderLayer layer;
        public BufferUploadData data;

        public MeshUpload(RenderLayer layer, BufferUploadData data) {
            this.layer = layer;
            this.data = data;
        }
    }
}