package me.jellysquid.mods.sodium.mixin.features.world.biome;

import net.minecraft.world.biome.Biome;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Biome.Weather.class)
public interface WeatherAccessor {
    @Accessor
    float getTemperature();

    @Accessor
    float getDownfall();
}
