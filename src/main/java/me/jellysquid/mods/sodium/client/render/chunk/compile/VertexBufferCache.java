package me.jellysquid.mods.sodium.client.render.chunk.compile;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.RenderLayer;

import java.util.HashMap;
import java.util.Map;

@Environment(EnvType.CLIENT)
public class VertexBufferCache {
    private final Map<RenderLayer, BufferBuilder> builders = new HashMap<>();

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
