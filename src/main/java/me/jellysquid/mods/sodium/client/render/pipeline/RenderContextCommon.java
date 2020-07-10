package me.jellysquid.mods.sodium.client.render.pipeline;

import me.jellysquid.mods.sodium.client.SodiumClientMod;
import me.jellysquid.mods.sodium.client.model.quad.blender.BiomeColorBlender;
import me.jellysquid.mods.sodium.client.model.quad.blender.FlatBiomeColorBlender;
import me.jellysquid.mods.sodium.client.model.quad.blender.SmoothBiomeColorBlender;

public class RenderContextCommon {
    public static BiomeColorBlender createBiomeColorBlender() {
        return SodiumClientMod.options().quality.biomeBlendDistance <= 0 ? new FlatBiomeColorBlender() : new SmoothBiomeColorBlender();
    }
}
