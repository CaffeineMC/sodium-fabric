package me.jellysquid.mods.sodium.render.terrain.light;

import me.jellysquid.mods.sodium.render.terrain.light.data.LightDataAccess;
import me.jellysquid.mods.sodium.render.terrain.light.flat.FlatLightPipeline;
import me.jellysquid.mods.sodium.render.terrain.light.smooth.SmoothLightPipeline;

import java.util.EnumMap;

public class LightPipelineProvider {
    private final EnumMap<LightMode, LightPipeline> lighters = new EnumMap<>(LightMode.class);

    public LightPipelineProvider(LightDataAccess cache) {
        this.lighters.put(LightMode.SMOOTH, new SmoothLightPipeline(cache));
        this.lighters.put(LightMode.FLAT, new FlatLightPipeline(cache));
    }

    public LightPipeline getLighter(LightMode type) {
        LightPipeline pipeline = this.lighters.get(type);

        if (pipeline == null) {
            throw new NullPointerException("No lighter exists for mode: " + type.name());
        }

        return pipeline;
    }
}
