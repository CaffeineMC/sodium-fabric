package me.jellysquid.mods.sodium.client.world;

import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.source.BiomeAccess;
import net.minecraft.world.biome.source.BiomeAccessType;

import java.util.concurrent.atomic.AtomicReferenceArray;

public class BiomeCache {
    private final BiomeAccessType type;
    private final long seed;

    private AtomicReferenceArray<Biome> biomes;

    public BiomeCache(BiomeAccessType type, long seed) {
        this.type = type;
        this.seed = seed;
        this.biomes = new AtomicReferenceArray<>(16 * 16 * 16);
    }

    public Biome getBiome(BiomeAccess.Storage storage, int x, int y, int z) {
        int idx = ((y & 15) << 8) | ((z & 15) << 4) | (x & 15);

        Biome biome = this.biomes.get(idx);

        if (biome == null) {
            this.biomes.compareAndSet(idx, null, biome = this.type.getBiome(this.seed, x, y, z, storage));
        }

        return biome;
    }

}
