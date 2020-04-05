package me.jellysquid.mods.sodium.client.world;

import it.unimi.dsi.fastutil.objects.Reference2ShortMap;
import it.unimi.dsi.fastutil.objects.Reference2ShortOpenHashMap;
import it.unimi.dsi.fastutil.objects.ReferenceArrayList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.source.BiomeAccess;
import net.minecraft.world.biome.source.BiomeAccessType;

public class BiomeCache {
    private final BiomeAccess.Storage storage;
    private final BiomeAccessType type;
    private final long seed;

    private ReferenceArrayList<Biome> biomeList;
    private Reference2ShortMap<Biome> biomeLookup;

    private short[] biomes;
    private short nextId = 1;

    public BiomeCache(BiomeAccess.Storage storage, BiomeAccessType type, long seed) {
        this.storage = storage;
        this.type = type;
        this.seed = seed;
    }

    public Biome getBiome(BlockPos pos) {
        return this.getBiome(pos.getX(), pos.getY(), pos.getZ());
    }

    public Biome getBiome(int x, int y, int z) {
        if (this.biomes == null) {
            this.init();
        }

        int idx = ((y & 15) << 8) | ((x & 15) << 4) | (z & 15);

        short id = this.biomes[idx];

        if (id == 0) {
            this.biomes[idx] = id = this.fetchBiome(x, y, z);
        }

        return this.biomeList.get(id);
    }

    private void init() {
        this.biomes = new short[16 * 16 * 16];
        this.biomeLookup = new Reference2ShortOpenHashMap<>();
        this.biomeList = new ReferenceArrayList<>();
        this.biomeList.add(null);
    }

    private short fetchBiome(int x, int y, int z) {
        Biome biome = this.type.getBiome(this.seed, x, y, z, this.storage);

        short id = this.biomeLookup.getShort(biome);

        if (id == 0) {
            id = this.allocateId(biome);
        }

        return id;
    }

    private short allocateId(Biome biome) {
        short id = this.nextId++;
        this.biomeLookup.put(biome, id);
        this.biomeList.add(biome);

        return id;
    }
}
