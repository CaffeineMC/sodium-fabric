package me.jellysquid.mods.sodium.client.model.light;

import me.jellysquid.mods.sodium.client.util.DirectionUtil;
import net.minecraft.util.math.Direction;
import net.minecraft.world.BlockRenderView;

public class SidedBrightnessTable {
    private final float[] table = new float[DirectionUtil.ALL_DIRECTIONS.length * 2];

    public SidedBrightnessTable(BlockRenderView world) {
        for (Direction dir : DirectionUtil.ALL_DIRECTIONS) {
            this.table[(dir.ordinal() * 2)] = world.getBrightness(dir, false);
            this.table[(dir.ordinal() * 2) + 1] = world.getBrightness(dir, true);
        }
    }

    public float get(Direction dir, boolean shaded) {
        return this.table[(dir.ordinal() * 2) + (shaded ? 1 : 0)];
    }
}
