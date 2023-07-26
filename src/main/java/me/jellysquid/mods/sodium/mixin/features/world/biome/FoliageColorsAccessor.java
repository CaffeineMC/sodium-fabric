package me.jellysquid.mods.sodium.mixin.features.world.biome;

import net.minecraft.client.color.world.FoliageColors;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(FoliageColors.class)
public interface FoliageColorsAccessor {
    @Accessor
    static int[] getColorMap() {
        throw new AssertionError();
    }
}