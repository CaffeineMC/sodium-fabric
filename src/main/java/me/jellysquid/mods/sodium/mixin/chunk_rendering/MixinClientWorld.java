package me.jellysquid.mods.sodium.mixin.chunk_rendering;

import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import me.jellysquid.mods.sodium.client.world.ClientWorldExtended;
import me.jellysquid.mods.sodium.client.world.SodiumChunkManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.BiomeColorCache;
import net.minecraft.client.world.ClientChunkManager;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.CuboidBlockIterator;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.biome.source.BiomeAccess;
import net.minecraft.world.level.ColorResolver;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ClientWorld.class)
public abstract class MixinClientWorld implements ClientWorldExtended {
    @Shadow
    @Final
    private Object2ObjectArrayMap<ColorResolver, BiomeColorCache> colorCache;

    @Redirect(method = "method_2940", at = @At(value = "NEW", target = "net/minecraft/client/world/ClientChunkManager"))
    private static ClientChunkManager redirectCreateChunkManager(ClientWorld world, int dist) {
        return new SodiumChunkManager(world, dist);
    }

    @Override
    public int getColor(BlockPos pos, ColorResolver resolver, BiomeAccess biomeAccess) {
        BiomeColorCache cache = this.colorCache.get(resolver);
        return cache.getBiomeColor(pos, () -> {
            return this.calculateColor(pos, resolver, biomeAccess);
        });
    }

    public int calculateColor(BlockPos pos, ColorResolver colorResolver_1, BiomeAccess biomeAccess) {
        int blend = MinecraftClient.getInstance().options.biomeBlendRadius;

        if (blend == 0) {
            return colorResolver_1.getColor(biomeAccess.getBiome(pos), pos.getX(), pos.getZ());
        }

        int int_2 = (blend * 2 + 1) * (blend * 2 + 1);

        int int_3 = 0;
        int int_4 = 0;
        int int_5 = 0;

        CuboidBlockIterator it = new CuboidBlockIterator(pos.getX() - blend, pos.getY(), pos.getZ() - blend, pos.getX() + blend, pos.getY(), pos.getZ() + blend);

        int color;

        for(BlockPos.Mutable npos = new BlockPos.Mutable(); it.step(); int_5 += color & 255) {
            npos.set(it.getX(), it.getY(), it.getZ());

            color = colorResolver_1.getColor(biomeAccess.getBiome(npos), npos.getX(), npos.getZ());

            int_3 += (color & 16711680) >> 16;
            int_4 += (color & '\uff00') >> 8;
        }

        return (int_3 / int_2 & 255) << 16 | (int_4 / int_2 & 255) << 8 | int_5 / int_2 & 255;
    }
}
