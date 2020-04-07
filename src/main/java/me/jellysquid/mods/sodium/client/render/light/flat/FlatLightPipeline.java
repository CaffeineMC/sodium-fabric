package me.jellysquid.mods.sodium.client.render.light.flat;

import me.jellysquid.mods.sodium.client.render.light.LightPipeline;
import me.jellysquid.mods.sodium.client.render.light.LightResult;
import me.jellysquid.mods.sodium.client.render.light.cache.LightDataCache;
import me.jellysquid.mods.sodium.client.render.model.quad.ModelQuadFlags;
import me.jellysquid.mods.sodium.client.render.model.quad.ModelQuadView;
import net.minecraft.util.math.BlockPos;

import java.util.Arrays;

import static me.jellysquid.mods.sodium.client.render.light.cache.LightDataCache.unpackLM;

public class FlatLightPipeline implements LightPipeline {
    private final LightDataCache lightCache;

    public FlatLightPipeline(LightDataCache cache) {
        this.lightCache = cache;
    }

    @Override
    public void reset() {

    }

    @Override
    public void apply(ModelQuadView quad, BlockPos pos, LightResult out) {
        if (this.lightCache == null) {
            throw new IllegalStateException("Light cache not defined");
        }

        Arrays.fill(out.br, 1.0f);

        if ((quad.getFlags() & ModelQuadFlags.IS_ALIGNED) != 0) {
            Arrays.fill(out.lm, unpackLM(this.lightCache.get(pos, quad.getFacing())));
        } else {
            Arrays.fill(out.lm, unpackLM(this.lightCache.get(pos)));
        }
    }
}
