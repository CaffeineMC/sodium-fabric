package me.jellysquid.mods.sodium.mixin.features.biome;

import me.jellysquid.mods.sodium.client.render.biome.BiomeColorMaps;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeEffects;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Biome.class)
public abstract class MixinBiome {
    @Shadow
    @Final
    private BiomeEffects effects;

    @Shadow
    public abstract float getTemperature();

    @Shadow
    public abstract float getDownfall();

    @Unique
    private boolean hasCustomGrassColor;

    @Unique
    private int customGrassColor;

    @Unique
    private boolean hasCustomFoliageColor;

    @Unique
    private int customFoliageColor;

    @Unique
    private int defaultColorIndex;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void setupColors(CallbackInfo ci) {
        var grassColor = this.effects.getGrassColor();

        if (grassColor.isPresent()) {
            this.hasCustomGrassColor = true;
            this.customGrassColor = grassColor.get();
        }

        var foliageColor = this.effects.getFoliageColor();

        if (foliageColor.isPresent()) {
            this.hasCustomFoliageColor = true;
            this.customFoliageColor = foliageColor.get();
        }

        this.defaultColorIndex = this.getDefaultColorIndex();
    }

    @Overwrite
    public int getGrassColorAt(double x, double z) {
        int color;

        if (this.hasCustomGrassColor) {
            color = this.customGrassColor;
        } else {
            color = BiomeColorMaps.getGrassColor(this.defaultColorIndex);
        }

        var modifier = this.effects.getGrassColorModifier();

        if (modifier != BiomeEffects.GrassColorModifier.NONE) {
            color = modifier.getModifiedGrassColor(x, z, color);
        }

        return color;
    }

    @Overwrite
    public int getFoliageColor() {
        int color;

        if (this.hasCustomFoliageColor) {
            color = this.customFoliageColor;
        } else {
            color = BiomeColorMaps.getFoliageColor(this.defaultColorIndex);
        }

        return color;
    }

    private int getDefaultColorIndex() {
        double temperature = MathHelper.clamp(this.getTemperature(), 0.0F, 1.0F);
        double humidity = MathHelper.clamp(this.getDownfall(), 0.0F, 1.0F);

        return BiomeColorMaps.getIndex(temperature, humidity);
    }
}
