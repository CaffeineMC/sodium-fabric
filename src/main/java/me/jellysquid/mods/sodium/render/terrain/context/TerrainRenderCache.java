package me.jellysquid.mods.sodium.render.terrain.context;

import me.jellysquid.mods.sodium.render.terrain.color.blender.ColorBlender;
import me.jellysquid.mods.sodium.render.terrain.color.blender.FlatColorBlender;
import me.jellysquid.mods.sodium.render.terrain.color.blender.LinearColorBlender;
import net.minecraft.client.MinecraftClient;

@Deprecated
public class TerrainRenderCache {
    protected ColorBlender createBiomeColorBlender() {
        return MinecraftClient.getInstance().options.getBiomeBlendRadius().getValue() <= 0 ? new FlatColorBlender() : new LinearColorBlender();
    }
}
