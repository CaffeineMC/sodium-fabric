package net.caffeinemc.sodium.world.biome;

import it.unimi.dsi.fastutil.objects.Reference2ReferenceOpenHashMap;
import net.caffeinemc.sodium.util.image.BoxBlur;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.registry.RegistryEntry;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.source.BiomeAccess;
import net.minecraft.world.level.ColorResolver;

import java.util.Map;

public class ChunkColorCache {
    private static final int BORDER = 1;

    private final RegistryEntry<Biome>[][] biomes;
    private final Map<ColorResolver, int[][]> colors;

    private final int sizeHorizontal;
    private final int sizeVertical;

    private final int blurHorizontal;

    private final int baseX, baseY, baseZ;

    private final ChunkSectionPos origin;
    private final BiomeAccess biomeAccess;

    public ChunkColorCache(ChunkSectionPos origin, BiomeAccess biomeAccess, int radius) {
        this.origin = origin;
        this.biomeAccess = biomeAccess;

        int borderXZ = radius + BORDER;
        int borderY = BORDER;

        this.sizeHorizontal = 16 + (borderXZ * 2);
        this.sizeVertical = 16 + (borderY * 2);

        this.blurHorizontal = radius;

        this.baseX = this.origin.getMinX() - borderXZ;
        this.baseY = this.origin.getMinY() - borderY;
        this.baseZ = this.origin.getMinZ() - borderXZ;

        this.colors = new Reference2ReferenceOpenHashMap<>();

        //noinspection unchecked
        this.biomes = new RegistryEntry[this.sizeVertical][];
    }

    public int getColor(ColorResolver resolver, int posX, int posY, int posZ) {
        var x = MathHelper.clamp(posX - this.baseX, 0, this.sizeHorizontal);
        var y = MathHelper.clamp(posY - this.baseY, 0, this.sizeVertical);
        var z = MathHelper.clamp(posZ - this.baseZ, 0, this.sizeHorizontal);

        int[][] colors = this.colors.get(resolver);

        if (colors == null) {
            this.colors.put(resolver, colors = new int[this.sizeVertical][]);
        }

        var layer = colors[y];

        if (layer == null) {
            colors[y] = (layer = this.gatherColorsXZ(resolver, y));
        }

        return layer[this.indexXZ(x, z)];
    }

    private RegistryEntry<Biome>[] gatherBiomes(int level) {
        @SuppressWarnings("unchecked")
        RegistryEntry<Biome>[] biomeData = new RegistryEntry[this.sizeHorizontal * this.sizeHorizontal];

        var pos = new BlockPos.Mutable();

        for (int x = 0; x < this.sizeHorizontal; x++) {
            for (int z = 0; z < this.sizeHorizontal; z++) {
                biomeData[this.indexXZ(x, z)] = this.biomeAccess.getBiome(pos.set(x + this.baseX, level + this.baseY, z + this.baseZ));
            }
        }

        return biomeData;
    }

    private int[] gatherColorsXZ(ColorResolver resolver, int y) {
        var biomeData = this.getBiomeData(y);
        var colorData = new int[this.sizeHorizontal * this.sizeHorizontal];

        for (int x = 0; x < this.sizeHorizontal; x++) {
            for (int z = 0; z < this.sizeHorizontal; z++) {
                int index = this.indexXZ(x, z);
                colorData[index] = resolver.getColor(biomeData[index].value(),
                        x + this.baseX, z + this.baseZ);
            }
        }

        BoxBlur.blur(colorData, this.sizeHorizontal, this.sizeHorizontal, this.blurHorizontal);

        return colorData;
    }

    private RegistryEntry<Biome>[] getBiomeData(int y) {
        var biomes = this.biomes[y];

        if (biomes == null) {
            this.biomes[y] = (biomes = this.gatherBiomes(y));
        }

        return biomes;
    }

    private int indexXZ(int x, int z) {
        return (x * this.sizeHorizontal) + z;
    }
}
