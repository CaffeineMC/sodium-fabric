package me.jellysquid.mods.sodium.client.render.light.flat;

import me.jellysquid.mods.sodium.client.render.LightDataCache;
import me.jellysquid.mods.sodium.client.render.light.LightPipeline;
import me.jellysquid.mods.sodium.client.render.light.LightResult;
import me.jellysquid.mods.sodium.client.render.quad.ModelQuadFlags;
import me.jellysquid.mods.sodium.client.render.quad.ModelQuadView;
import net.minecraft.util.math.BlockPos;

import java.util.Arrays;

public class FlatLightPipeline implements LightPipeline {
    private final LightDataCache lightCache;

    public FlatLightPipeline(LightDataCache lightCache) {
        this.lightCache = lightCache;
    }

    @Override
    public void reset() {

    }

    @Override
    public void apply(ModelQuadView quad, BlockPos pos, LightResult out) {
        Arrays.fill(out.br, 1.0f);

        if ((quad.getFlags() & ModelQuadFlags.IS_ALIGNED) != 0) {
            Arrays.fill(out.lm, LightDataCache.unpackLM(this.lightCache.get(pos, quad.getFacing())));
        } else {
            Arrays.fill(out.lm, LightDataCache.unpackLM(this.lightCache.get(pos)));
        }
    }
}
