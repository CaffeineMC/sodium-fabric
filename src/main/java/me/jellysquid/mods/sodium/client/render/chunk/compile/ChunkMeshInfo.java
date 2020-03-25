package me.jellysquid.mods.sodium.client.render.chunk.compile;

import it.unimi.dsi.fastutil.objects.ObjectArraySet;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.chunk.ChunkOcclusionData;
import net.minecraft.util.math.Direction;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class ChunkMeshInfo {
    private static final ChunkMeshInfo EMPTY = new ChunkMeshInfo();

    public static ChunkMeshInfo empty() {
        return EMPTY;
    }

    protected final Set<RenderLayer> presentLayers = new ObjectArraySet<>();

    protected final List<BlockEntity> globalEntities = new ArrayList<>();
    protected final List<BlockEntity> blockEntities = new ArrayList<>();

    protected ChunkOcclusionData occlusionGraph;
    protected BufferBuilder.State translucentBufferState;

    protected boolean empty;

    public boolean isEmpty() {
        return this.empty;
    }

    public boolean containsLayer(RenderLayer layer) {
        return this.presentLayers.contains(layer);
    }

    public List<BlockEntity> getBlockEntities() {
        return this.blockEntities;
    }

    public boolean isVisibleThrough(Direction from, Direction to) {
        return this.occlusionGraph != null && this.occlusionGraph.isVisibleThrough(from, to);
    }
}