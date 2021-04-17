package me.jellysquid.mods.sodium.mixin.features.chunk_rendering;

import me.jellysquid.mods.sodium.client.world.ClientWorldExtended;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.world.ClientChunkManager;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.profiler.Profiler;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionType;
import org.spongepowered.asm.mixin.Dynamic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Supplier;

@Mixin(ClientWorld.class)
public abstract class MixinClientWorld implements ClientWorldExtended {
    private long biomeSeed;

    /**
     * Captures the biome generation seed so that our own caches can make use of it.
     */
    @Inject(method = "<init>", at = @At("RETURN"))
    private void init(ClientPlayNetworkHandler clientPlayNetworkHandler, ClientWorld.Properties properties, RegistryKey<World> registryKey, DimensionType dimensionType, int i, Supplier<Profiler> supplier, WorldRenderer worldRenderer, boolean bl, long seed, CallbackInfo ci) {
        this.biomeSeed = seed;
    }

    @Override
    public long getBiomeSeed() {
        return this.biomeSeed;
    }
}
