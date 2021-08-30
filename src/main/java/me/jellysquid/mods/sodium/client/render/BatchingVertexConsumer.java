package me.jellysquid.mods.sodium.client.render;

import it.unimi.dsi.fastutil.objects.*;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class BatchingVertexConsumer implements VertexConsumerProvider {
    // Defines the order in which render layers will be drawn
    // This is necessary for overlay rendering (such as that for item glint)
    private static final Reference2IntMap<RenderLayer> LAYER_PRIORITY = new Reference2IntOpenHashMap<>();

    // Vanilla provides unreasonable sizes (2 MiB+) for some buffers that are shared with chunk rendering
    // We instead use our own size hint to avoid excessive allocations
    private static final int DEFAULT_BUFFER_SIZE = 65536;

    static {
        LAYER_PRIORITY.defaultReturnValue(10);
        LAYER_PRIORITY.put(RenderLayer.getDirectGlint(), 1000);
    }

    private final Map<RenderLayer, BufferBuilder> layerBuffers = new Reference2ObjectOpenHashMap<>();
    private final Set<RenderLayer> activeLayers = new ReferenceOpenHashSet<>();

    @Override
    public VertexConsumer getBuffer(RenderLayer renderLayer) {
        BufferBuilder bufferBuilder = this.getOrCreateBuffer(renderLayer);

        if (this.activeLayers.add(renderLayer)) {
            bufferBuilder.begin(renderLayer.getDrawMode(), renderLayer.getVertexFormat());
        }

        return bufferBuilder;
    }

    private BufferBuilder getOrCreateBuffer(RenderLayer layer) {
        return this.layerBuffers.computeIfAbsent(layer, BatchingVertexConsumer::createBufferBuilder);
    }

    public void draw() {
        List<RenderLayer> sorted = new ObjectArrayList<>(this.activeLayers);
        sorted.sort(Comparator.comparingInt(LAYER_PRIORITY::getInt));

        for (RenderLayer layer : sorted) {
            this.drawLayer(layer);
        }
    }

    private void drawLayer(RenderLayer layer) {
        BufferBuilder bufferBuilder = this.layerBuffers.get(layer);

        if (bufferBuilder != null) {
            layer.draw(bufferBuilder, 0, 0, 0);
        }

        this.activeLayers.remove(layer);
    }

    private static BufferBuilder createBufferBuilder(RenderLayer layer) {
        return new BufferBuilder(DEFAULT_BUFFER_SIZE);
    }
}
