package me.jellysquid.mods.sodium.render.terrain.context;

import me.jellysquid.mods.sodium.render.terrain.color.blender.ColorBlender;
import me.jellysquid.mods.sodium.render.terrain.color.blender.FlatColorBlender;
import me.jellysquid.mods.sodium.render.terrain.color.blender.LinearColorBlender;
import net.minecraft.client.Minecraft;

@Deprecated
public class TerrainRenderCache {
    protected ColorBlender createBiomeColorBlender() {
        return Minecraft.getInstance().options.biomeBlendRadius <= 0 ? new FlatColorBlender() : new LinearColorBlender();
    }
}
