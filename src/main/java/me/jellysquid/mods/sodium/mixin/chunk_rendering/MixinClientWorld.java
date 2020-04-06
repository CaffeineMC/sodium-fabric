package me.jellysquid.mods.sodium.mixin.chunk_rendering;

import me.jellysquid.mods.sodium.client.world.BiomeCacheManager;
import me.jellysquid.mods.sodium.client.world.ClientWorldExtended;
import me.jellysquid.mods.sodium.client.world.SodiumChunkManager;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.world.ClientChunkManager;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.profiler.Profiler;
import net.minecraft.world.World;
import net.minecraft.world.chunk.ChunkManager;
import net.minecraft.world.dimension.Dimension;
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.world.level.LevelInfo;
import net.minecraft.world.level.LevelProperties;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.BiFunction;

@Mixin(ClientWorld.class)
public abstract class MixinClientWorld extends World implements ClientWorldExtended {
    private BiomeCacheManager biomeCacheManager;

    protected MixinClientWorld(LevelProperties properties, DimensionType dimensionType, BiFunction<World, Dimension, ChunkManager> chunkManagerLoader, Profiler profiler, boolean client) {
        super(properties, dimensionType, chunkManagerLoader, profiler, client);
    }

    @Inject(method = "<init>", at = @At("RETURN"))
    private void init(ClientPlayNetworkHandler netHandler, LevelInfo levelInfo, DimensionType dimensionType, int renderDistance, Profiler profiler, WorldRenderer worldRenderer, CallbackInfo ci) {
        this.biomeCacheManager = new BiomeCacheManager(this.getDimension().getType().getBiomeAccessType(), this.getSeed());
    }

    @Redirect(method = "method_2940", at = @At(value = "NEW", target = "net/minecraft/client/world/ClientChunkManager"))
    private static ClientChunkManager redirectCreateChunkManager(ClientWorld world, int renderDistance) {
        return new SodiumChunkManager(world, renderDistance);
    }

    @Override
    public BiomeCacheManager getBiomeCacheManager() {
        return this.biomeCacheManager;
    }

    @Inject(method = "resetChunkColor", at = @At("HEAD"))
    private void onChunkColorReset(int x, int z, CallbackInfo ci) {
        this.biomeCacheManager.clearCacheFor(x, z);
    }
}
