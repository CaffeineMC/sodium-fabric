package me.jellysquid.mods.sodium.client.model.light;

import me.jellysquid.mods.sodium.client.model.light.cache.LightDataCache;
import net.minecraft.util.math.Direction;

public abstract class AbstractLightPipeline implements LightPipeline {
    /**
     * The cache which light data will be accessed from.
     */
    protected final LightDataCache lightCache;

    public AbstractLightPipeline(LightDataCache lightCache) {
        this.lightCache = lightCache;
    }

    protected void applySidedBrightnessModifier(float[] arr, Direction face, boolean shade) {
        float mod = this.lightCache.getWorld().getBrightness(face, shade);

        for (int i = 0; i < arr.length; i++) {
            arr[i] *= mod;
        }
    }
}
