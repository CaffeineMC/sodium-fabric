package me.jellysquid.mods.sodium.client.world.biome;

import me.jellysquid.mods.sodium.client.world.ClientWorldExtended;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.biome.BiomeZoomer;
import java.util.Arrays;

public class BiomeCache {
    private final BiomeZoomer type;
    private final long seed;

    private final Biome[] biomes;

    public BiomeCache(Level world) {
        this.type = world.dimensionType().getBiomeZoomer();
        this.seed = ((ClientWorldExtended) world).getBiomeSeed();

        this.biomes = new Biome[16 * 16];
    }

    public Biome getBiome(BiomeManager.NoiseBiomeSource storage, int x, int y, int z) {
        int idx = ((z & 15) << 4) | (x & 15);

        Biome biome = this.biomes[idx];

        if (biome == null) {
            this.biomes[idx] = biome = this.type.getBiome(this.seed, x, y, z, storage);
        }

        return biome;
    }

    public void reset() {
        Arrays.fill(this.biomes, null);
    }
}
