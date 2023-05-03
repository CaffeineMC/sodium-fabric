package me.jellysquid.mods.sodium.mixin.features.fast_biome_colors;

import me.jellysquid.mods.sodium.client.util.color.FastCubicSampler;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.CubicSampler;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.function.Function;

@Mixin(ClientWorld.class)
public class MixinClientWorld {
    @Redirect(method = "getSkyColor", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/CubicSampler;sampleColor(Lnet/minecraft/util/math/Vec3d;Lnet/minecraft/util/CubicSampler$RgbFetcher;)Lnet/minecraft/util/math/Vec3d;"))
    private Vec3d redirectSampleColor(Vec3d pos, CubicSampler.RgbFetcher rgbFetcher) {
        World world = (World) (Object) this;

        return FastCubicSampler.sampleColor(pos, (x, y, z) -> world.getBiomeForNoiseGen(x, y, z).value().getSkyColor(), Function.identity());
    }
}
