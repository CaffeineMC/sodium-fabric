package net.caffeinemc.sodium.render.terrain.context;

import net.caffeinemc.sodium.render.terrain.color.blender.ColorBlender;
import net.caffeinemc.sodium.render.terrain.color.blender.FlatColorBlender;
import net.caffeinemc.sodium.render.terrain.color.blender.LinearColorBlender;
import net.minecraft.client.MinecraftClient;

@Deprecated
public class TerrainRenderCache {
    protected ColorBlender createBiomeColorBlender() {
        return MinecraftClient.getInstance().options.getBiomeBlendRadius().getValue() <= 0 ? new FlatColorBlender() : new LinearColorBlender();
    }
}
