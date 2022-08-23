package me.jellysquid.mods.sodium.client.render.pipeline;

import me.jellysquid.mods.sodium.client.model.quad.blender.ColorBlender;
import me.jellysquid.mods.sodium.client.model.quad.blender.FlatColorBlender;
import me.jellysquid.mods.sodium.client.model.quad.blender.LinearColorBlender;
import net.minecraft.client.MinecraftClient;

public class ChunkRenderCache {
    protected ColorBlender createBiomeColorBlender() {
        return MinecraftClient.getInstance().options.getBiomeBlendRadius().getValue() <= 0 ? new FlatColorBlender() : new LinearColorBlender();
    }
}
