package me.jellysquid.mods.sodium.mixin.pipeline;

import me.jellysquid.mods.sodium.client.render.pipeline.ExtendedChunkData;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.chunk.ChunkBuilder;
import net.minecraft.client.render.chunk.ChunkOcclusionData;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Set;

@Mixin(ChunkBuilder.ChunkData.class)
public class MixinChunkData implements ExtendedChunkData {
    @Shadow
    @Final
    private Set<RenderLayer> nonEmptyLayers;

    @Shadow
    @Final
    private Set<RenderLayer> initializedLayers;

    @Shadow
    private boolean empty;

    @Shadow
    private BufferBuilder.State bufferState;

    @Shadow
    private ChunkOcclusionData occlusionGraph;

    @Override
    public Set<RenderLayer> getNonEmptyLayers() {
        return this.nonEmptyLayers;
    }

    @Override
    public Set<RenderLayer> getInitializedLayers() {
        return this.initializedLayers;
    }

    @Override
    public void markNonEmpty() {
        this.empty = false;
    }

    @Override
    public void setTranslucentBufferState(BufferBuilder.State state) {
        this.bufferState = state;
    }

    @Override
    public void setOcclusionData(ChunkOcclusionData data) {
        this.occlusionGraph = data;
    }
}
