package me.jellysquid.mods.sodium.client.render.chunk.compile;

import it.unimi.dsi.fastutil.objects.Reference2ReferenceArrayMap;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.RenderLayer;

@Environment(EnvType.CLIENT)
public class VertexBufferCache {
    private final Reference2ReferenceArrayMap<RenderLayer, BufferBuilder> builders = new Reference2ReferenceArrayMap<>();

    public VertexBufferCache() {
        for (RenderLayer layer : RenderLayer.getBlockLayers()) {
            this.builders.put(layer, new BufferBuilder(layer.getExpectedBufferSize()));
        }
    }

    public BufferBuilder get(RenderLayer layer) {
        return this.builders.get(layer);
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
}
