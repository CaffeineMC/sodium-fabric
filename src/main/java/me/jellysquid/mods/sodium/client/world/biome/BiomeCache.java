package me.jellysquid.mods.sodium.client.world.biome;

import me.jellysquid.mods.sodium.client.world.ClientWorldExtended;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.source.BiomeAccess;

import java.util.Arrays;

// This class is unused due to 1.18 changes breaking it majorly, it will have to be looked into if this can be salvaged.
public class BiomeCache {
    private final BiomeAccess type;
    private final World world;

    private final Biome[] biomes;

    public BiomeCache(World world) {
        this.type = world.getBiomeAccess();
        this.world = world;

        this.biomes = new Biome[16 * 16 * world.countVerticalSections()];
    }

    public Biome getBiome(BiomeAccess.Storage storage, int x, int y, int z) {
        int idx = ((z & 15) << 8) | ((y & world.countVerticalSections() - 1) << 4) | (x & 15);

        Biome biome = this.biomes[idx];

        if (biome == null) {
            this.biomes[idx] = biome = this.type.getBiome(new BlockPos(x, y, z));
        }

        return biome;
    }

    public void reset() {
        Arrays.fill(this.biomes, null);
    }
}
