package me.jellysquid.mods.sodium.mixin.features.fast_biome_colors;

import me.jellysquid.mods.sodium.client.util.color.FastCubicSampler;
import net.minecraft.client.render.BackgroundRenderer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.CubicSampler;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(BackgroundRenderer.class)
public class MixinBackgroundRenderer {
    @Redirect(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/CubicSampler;sampleColor(Lnet/minecraft/util/math/Vec3d;Lnet/minecraft/util/CubicSampler$RgbFetcher;)Lnet/minecraft/util/math/Vec3d;"))
    private static Vec3d redirectSampleColor(Vec3d pos, CubicSampler.RgbFetcher rgbFetcher, Camera camera, float tickDelta, ClientWorld world, int i, float f) {
        float u = MathHelper.clamp(MathHelper.cos(world.getSkyAngle(tickDelta) * 6.2831855F) * 2.0F + 0.5F, 0.0F, 1.0F);

        return FastCubicSampler.sampleColor(pos,
                (x, y, z) -> world.getBiomeAccess().getBiomeForNoiseGen(x, y, z).getFogColor(),
                (v) -> world.getSkyProperties().adjustFogColor(v, u));
    }
}
