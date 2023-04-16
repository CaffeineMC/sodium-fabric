package me.jellysquid.mods.sodium.mixin.features.chunk_rendering;

import me.jellysquid.mods.sodium.client.render.SodiumWorldRenderer;
import me.jellysquid.mods.sodium.client.world.BiomeSeedProvider;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Supplier;

@Mixin(ClientWorld.class)
public class MixinClientWorld implements BiomeSeedProvider {
    @Unique
    private long biomeSeed;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void captureSeed(ClientPlayNetworkHandler netHandler, ClientWorld.Properties properties, RegistryKey<World> registryRef, RegistryEntry registryEntry, int loadDistance, int simulationDistance, Supplier profiler, WorldRenderer worldRenderer, boolean debugWorld, long seed, CallbackInfo ci) {
        this.biomeSeed = seed;
    }

    @Override
    public long getBiomeSeed() {
        return biomeSeed;
    }
}
