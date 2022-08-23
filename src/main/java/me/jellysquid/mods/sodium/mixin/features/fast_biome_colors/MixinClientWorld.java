package me.jellysquid.mods.sodium.mixin.features.fast_biome_colors;

import me.jellysquid.mods.sodium.client.util.color.FastCubicSampler;
import me.jellysquid.mods.sodium.client.world.BiomeSeedProvider;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.CubicSampler;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.registry.RegistryEntry;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Function;
import java.util.function.Supplier;

@Mixin(ClientWorld.class)
public class MixinClientWorld implements BiomeSeedProvider {
    @Unique
    private long biomeSeed;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void captureSeed(ClientPlayNetworkHandler netHandler, ClientWorld.Properties properties, RegistryKey<World> registryRef, RegistryEntry registryEntry, int loadDistance, int simulationDistance, Supplier profiler, WorldRenderer worldRenderer, boolean debugWorld, long seed, CallbackInfo ci) {
        this.biomeSeed = seed;
    }

    @Redirect(method = "getSkyColor", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/CubicSampler;sampleColor(Lnet/minecraft/util/math/Vec3d;Lnet/minecraft/util/CubicSampler$RgbFetcher;)Lnet/minecraft/util/math/Vec3d;"))
    private Vec3d redirectSampleColor(Vec3d pos, CubicSampler.RgbFetcher rgbFetcher) {
        World world = (World) (Object) this;

        return FastCubicSampler.sampleColor(pos, (x, y, z) -> world.getBiomeForNoiseGen(x, y, z).value().getSkyColor(), Function.identity());
    }

    @Override
    public long getBiomeSeed() {
        return this.biomeSeed;
    }
}
