package me.jellysquid.mods.sodium.mixin.core.world.biome;

import me.jellysquid.mods.sodium.client.world.BiomeSeedProvider;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.DimensionType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Supplier;

@Mixin(ClientLevel.class)
public class ClientWorldMixin implements BiomeSeedProvider {
    @Unique
    private long biomeSeed;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void captureSeed(ClientPacketListener networkHandler,
                             ClientLevel.ClientLevelData properties,
                             ResourceKey<Level> registryRef,
                             Holder<DimensionType> dimensionTypeEntry,
                             int loadDistance,
                             int simulationDistance,
                             Supplier<ProfilerFiller> profiler,
                             LevelRenderer worldRenderer,
                             boolean debugWorld,
                             long seed,
                             CallbackInfo ci) {
        this.biomeSeed = seed;
    }

    @Override
    public long sodium$getBiomeSeed() {
        return this.biomeSeed;
    }
}