package me.jellysquid.mods.sodium.mixin.features.world.biome;

import net.minecraft.client.color.world.GrassColors;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(GrassColors.class)
public interface GrassColorsAccessor {
    @Accessor
    static int[] getColorMap() {
        throw new AssertionError();
    }
}
