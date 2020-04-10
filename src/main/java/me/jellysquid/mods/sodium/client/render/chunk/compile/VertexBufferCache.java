package me.jellysquid.mods.sodium.client.render.chunk.compile;

import it.unimi.dsi.fastutil.objects.Reference2ReferenceArrayMap;
import me.jellysquid.mods.sodium.client.render.layer.BlockRenderPass;
import me.jellysquid.mods.sodium.client.render.layer.BlockRenderPassManager;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.RenderLayer;

import java.util.Map;

@Environment(EnvType.CLIENT)
public class VertexBufferCache {
    private final Reference2ReferenceArrayMap<BlockRenderPass, BufferBuilder> builders = new Reference2ReferenceArrayMap<>();
    private final BlockRenderPassManager renderPassManager;

    public VertexBufferCache(BlockRenderPassManager renderPassManager) {
        this.renderPassManager = renderPassManager;

        for (RenderLayer layer : RenderLayer.getBlockLayers()) {
            this.builders.put(renderPassManager.get(layer), new BufferBuilder(layer.getExpectedBufferSize()));
        }
    }

    public BufferBuilder get(RenderLayer layer) {
        return this.builders.get(this.renderPassManager.get(layer));
    }

    public void clear() {
        for (BufferBuilder builder : this.builders.values()) {
            builder.clear();
        }
    }

    public void reset() {
        for (BufferBuilder builder : this.builders.values()) {
            builder.reset();
        }
    }

    public Iterable<Map.Entry<BlockRenderPass, BufferBuilder>> getAllBuffers() {
        return this.builders.entrySet();
    }
}
