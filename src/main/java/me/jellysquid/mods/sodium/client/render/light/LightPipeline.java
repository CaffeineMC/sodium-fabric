package me.jellysquid.mods.sodium.client.render.light;

import me.jellysquid.mods.sodium.client.render.model.quad.ModelQuadView;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

public interface LightPipeline {
    void reset();

    void apply(ModelQuadView quad, BlockPos pos, LightResult out, Direction face);
}
