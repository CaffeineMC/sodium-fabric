package me.jellysquid.mods.sodium.client.render.pipeline;

import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.chunk.ChunkOcclusionData;

import java.util.Set;

public interface ExtendedChunkData {
    Set<RenderLayer> getNonEmptyLayers();

    Set<RenderLayer> getInitializedLayers();

    void markNonEmpty();

    void setTranslucentBufferState(BufferBuilder.State state);

    void setOcclusionData(ChunkOcclusionData data);
}
