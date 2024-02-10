package net.caffeinemc.mods.sodium.client.world.biome;

import net.caffeinemc.mods.sodium.client.world.BiomeSeedProvider;
import net.caffeinemc.mods.sodium.client.world.LevelSlice;
import net.caffeinemc.mods.sodium.client.world.cloned.ChunkRenderContext;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.LinearCongruentialGenerator;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;

/**
 * A slice of the level's biome data, with additional lookup tables to speed up queries.
 */
public class LevelBiomeSlice {
    /**
     * The resource key identifying the default biome of a chunk when it does not contain any biome data.
     */
    private static final ResourceKey<Biome> DEFAULT_BIOME_KEY = Biomes.PLAINS;

    /**
     * The size of the data arrays in each dimension.
     */
    private static final int SIZE = 3 * 4; // 3 chunks * 4 biomes per chunk

    // Arrays are in ZYX order
    @SuppressWarnings("unchecked")
    private final Holder<Biome>[] biomes = new Holder[SIZE * SIZE * SIZE];
    private final boolean[] uniform = new boolean[SIZE * SIZE * SIZE];
    private final BiasMap bias = new BiasMap();

    private long biomeZoomSeed;

    /**
     * The block position which the slice begins at.
     */
    private int originBlockX, originBlockY, originBlockZ;

    /**
     * Re-calculates the lookup tables used for biome queries. This should be called when the {@link LevelSlice}
     * is updated with new data.
     *
     * @param level The level which contains the zoom seed and registries for biomes
     * @param context The context which contains cloned biome data from chunks
     */
    public void update(ClientLevel level, ChunkRenderContext context) {
        this.originBlockX = context.getOrigin().minBlockX() - 16;
        this.originBlockY = context.getOrigin().minBlockY() - 16;
        this.originBlockZ = context.getOrigin().minBlockZ() - 16;

        this.biomeZoomSeed = BiomeSeedProvider.getBiomeZoomSeed(level);

        var registries = level.registryAccess();
        this.copyBiomeData(registries.registryOrThrow(Registries.BIOME), context);

        this.calculateBias();
        this.calculateUniform();
    }

    /**
     * Copies all biome data from the cloned chunk sections into the slice.
     * @param registry The registry to look up biome values from
     * @param context The cloned chunk sections to copy biome data from
     */
    private void copyBiomeData(Registry<Biome> registry, ChunkRenderContext context) {
        var defaultValue = registry.getHolderOrThrow(DEFAULT_BIOME_KEY);

        for (int sectionX = 0; sectionX < 3; sectionX++) {
            for (int sectionY = 0; sectionY < 3; sectionY++) {
                for (int sectionZ = 0; sectionZ < 3; sectionZ++) {
                    this.copySectionBiomeData(context, sectionX, sectionY, sectionZ, defaultValue);
                }
            }
        }
    }

    /**
     * Unpacks the biome data for a given chunk section into the slice.
     * @param context The render context holding cloned chunk sections which will be unpacked from
     * @param sectionX The x-coordinate of the section's position
     * @param sectionY The y-coordinate of the section's position
     * @param sectionZ The z-coordinate of the section's position
     * @param defaultBiome The default biome to unpack using, if the chunk section has no biome data
     */
    private void copySectionBiomeData(ChunkRenderContext context, int sectionX, int sectionY, int sectionZ, Holder<Biome> defaultBiome) {
        var section = context.getSections()[LevelSlice.getLocalSectionIndex(sectionX, sectionY, sectionZ)];
        var biomeData = section.getBiomeData();

        for (int relCellX = 0; relCellX < 4; relCellX++) {
            for (int relCellY = 0; relCellY < 4; relCellY++) {
                for (int relCellZ = 0; relCellZ < 4; relCellZ++) {
                    int cellX = (sectionX * 4) + relCellX;
                    int cellY = (sectionY * 4) + relCellY;
                    int cellZ = (sectionZ * 4) + relCellZ;

                    var idx = dataArrayIndex(cellX, cellY, cellZ);

                    if (biomeData == null) {
                        this.biomes[idx] = defaultBiome;
                    } else {
                        this.biomes[idx] = biomeData.get(relCellX, relCellY, relCellZ);
                    }
                }
            }
        }
    }

    /**
     * Computes the "uniformity" lookup table for each biome cell. A biome cell is considered to be uniform if all
     * neighboring cells are of the same biome. This allows for an optimization where neighboring biome queries can
     * be skipped in areas without an edge between different biomes (which is most of the time.)
     */
    private void calculateUniform() {
        for (int cellX = 2; cellX < 10; cellX++) {
            for (int cellY = 2; cellY < 10; cellY++) {
                for (int cellZ = 2; cellZ < 10; cellZ++) {
                    this.uniform[dataArrayIndex(cellX, cellY, cellZ)] = this.hasUniformNeighbors(cellX, cellY, cellZ);
                }
            }
        }
    }

    /**
     * Computes the bias vector lookup table for each biome cell. The bias is expensive to calculate, and querying a
     * biome involves calculating this bias for all other neighboring cells. Preparing the bias values ahead of time
     * allows for a significant reduction in overhead.
     */
    private void calculateBias() {
        int originX = this.originBlockX >> 2;
        int originY = this.originBlockY >> 2;
        int originZ = this.originBlockZ >> 2;

        final long seed = this.biomeZoomSeed;

        for (int relCellX = 1; relCellX < 11; relCellX++) {
            int cellX = originX + relCellX;
            long seedX = LinearCongruentialGenerator.next(seed, cellX);

            for (int relCellY = 1; relCellY < 11; relCellY++) {
                int cellY = originY + relCellY;
                long seedXY = LinearCongruentialGenerator.next(seedX, cellY);

                for (int relCellZ = 1; relCellZ < 11; relCellZ++) {
                    int cellZ = originZ + relCellZ;
                    long seedXYZ = LinearCongruentialGenerator.next(seedXY, cellZ);

                    this.calculateBias(dataArrayIndex(relCellX, relCellY, relCellZ),
                            cellX, cellY, cellZ, seedXYZ);
                }
            }
        }

    }

    /**
     * Calculates the bias vector for a given cell, and updates the lookup table for it. This vector is calculated from
     * a LCG which mixes the cell's coordinates with the voronoi zoom seed.
     *
     * @param cellIndex The data array index of the cell being calculated
     * @param cellX The x-coordinate of the cell
     * @param cellY The y-coordinate of the cell
     * @param cellZ The z-coordinate of the cell
     * @param seed The voronoi zoom seed as provided by {@link BiomeSeedProvider#sodium$getBiomeZoomSeed()}
     */
    private void calculateBias(int cellIndex, int cellX, int cellY, int cellZ, long seed) {
        seed = LinearCongruentialGenerator.next(seed, cellX);
        seed = LinearCongruentialGenerator.next(seed, cellY);
        seed = LinearCongruentialGenerator.next(seed, cellZ);

        var gradX = getBias(seed); seed = LinearCongruentialGenerator.next(seed, this.biomeZoomSeed);
        var gradY = getBias(seed); seed = LinearCongruentialGenerator.next(seed, this.biomeZoomSeed);
        var gradZ = getBias(seed);

        this.bias.set(cellIndex, gradX, gradY, gradZ);
    }

    /**
     * Calculates whether all neighbors of the given cell are have the same biome as itself.
     *
     * @param cellX The x-coordinate of the cell
     * @param cellY The y-coordinate of the cell
     * @param cellZ The z-coordinate of the cell
     * @return True if the neighbors are uniform, otherwise false
     */
    private boolean hasUniformNeighbors(int cellX, int cellY, int cellZ) {
        Biome biome = this.biomes[dataArrayIndex(cellX, cellY, cellZ)].value();

        int cellMinX = cellX - 1, cellMaxX = cellX + 1;
        int cellMinY = cellY - 1, cellMaxY = cellY + 1;
        int cellMinZ = cellZ - 1, cellMaxZ = cellZ + 1;

        for (int adjCellX = cellMinX; adjCellX <= cellMaxX; adjCellX++) {
            for (int adjCellY = cellMinY; adjCellY <= cellMaxY; adjCellY++) {
                for (int adjCellZ = cellMinZ; adjCellZ <= cellMaxZ; adjCellZ++) {
                    if (this.biomes[dataArrayIndex(adjCellX, adjCellY, adjCellZ)].value() != biome) {
                        return false;
                    }
                }
            }
        }

        return true;
    }

    /**
     * Calculates the biome for a given block position. The implementation of this function is intended to return
     * identical results to {@link net.minecraft.world.level.biome.BiomeManager#getBiome(BlockPos)}.
     *
     * @param blockX The x-coordinate of the block position
     * @param blockY The y-coordinate of the block position
     * @param blockZ The z-coordinate of the block position
     * @return The biome for the given block position, or the default if the chunk does not specify biome data
     */
    public Holder<Biome> getBiome(int blockX, int blockY, int blockZ) {
        int relBlockX = blockX - this.originBlockX;
        int relBlockY = blockY - this.originBlockY;
        int relBlockZ = blockZ - this.originBlockZ;

        int centerIndex = dataArrayIndex(
                QuartPos.fromBlock(relBlockX - 2),
                QuartPos.fromBlock(relBlockY - 2),
                QuartPos.fromBlock(relBlockZ - 2));

        // If all neighbors are uniform, then performing the voronoi search is not necessary
        if (this.uniform[centerIndex]) {
            return this.biomes[centerIndex];
        }

        // Slow path!
        return this.getBiomeUsingVoronoi(relBlockX, relBlockY, relBlockZ);
    }

    private Holder<Biome> getBiomeUsingVoronoi(int blockX, int blockY, int blockZ) {
        int x = blockX - 2;
        int y = blockY - 2;
        int z = blockZ - 2;

        int originIntX = QuartPos.fromBlock(x);
        int originIntY = QuartPos.fromBlock(y);
        int originIntZ = QuartPos.fromBlock(z);

        float originFracX = QuartPos.quartLocal(x) * 0.25f;
        float originFracY = QuartPos.quartLocal(y) * 0.25f;
        float originFracZ = QuartPos.quartLocal(z) * 0.25f;

        float closestDistance = Float.POSITIVE_INFINITY;
        int closestArrayIndex = 0;

        // Find the closest Voronoi cell to the given block position
        // The distance is calculated between center positions, which are offset by the bias parameter
        // The bias is pre-computed and stored for each cell
        for (int index = 0; index < 8; index++) {
            boolean dirX = (index & 4) != 0;
            boolean dirY = (index & 2) != 0;
            boolean dirZ = (index & 1) != 0;

            int cellIntX = originIntX + (dirX ? 1 : 0);
            int cellIntY = originIntY + (dirY ? 1 : 0);
            int cellIntZ = originIntZ + (dirZ ? 1 : 0);

            float cellFracX = originFracX - (dirX ? 1.0f : 0.0f);
            float cellFracY = originFracY - (dirY ? 1.0f : 0.0f);
            float cellFracZ = originFracZ - (dirZ ? 1.0f : 0.0f);

            int biasIndex = dataArrayIndex(cellIntX, cellIntY, cellIntZ);

            float biasX = biasToVector(this.bias.getX(biasIndex));
            float biasY = biasToVector(this.bias.getY(biasIndex));
            float biasZ = biasToVector(this.bias.getZ(biasIndex));

            float distanceX = Mth.square(cellFracX + biasX);
            float distanceY = Mth.square(cellFracY + biasY);
            float distanceZ = Mth.square(cellFracZ + biasZ);

            float distance = distanceX + distanceY + distanceZ;

            if (closestDistance > distance) {
                closestArrayIndex = biasIndex;
                closestDistance = distance;
            }
        }

        return this.biomes[closestArrayIndex];
    }

    private static int dataArrayIndex(int cellX, int cellY, int cellZ) {
        return (cellX * SIZE * SIZE) + (cellY * SIZE) + cellZ;
    }

    // Computes a vector position using the given bias. This normalizes the bias
    // into the range of [0.0, 0.9).
    private static float biasToVector(int bias) {
        return (bias * (1.0f / 1024.0f)) * 0.9f;
    }

    // Computes the bias value using the seed.
    // The seed should be re-mixed after calling this.
    private static int getBias(long l) {
        return (int) (((l >> 24) & 1023) - 512);
    }

    public static class BiasMap {
        // Pack the bias values for each axis into one array to keep things in cache.
        private final short[] data = new short[SIZE * SIZE * SIZE * 3];

        public void set(int index, int x, int y, int z) {
            this.data[(index * 3) + 0] = (short) x;
            this.data[(index * 3) + 1] = (short) y;
            this.data[(index * 3) + 2] = (short) z;
        }

        public int getX(int index) {
            return this.data[(index * 3) + 0];
        }

        public int getY(int index) {
            return this.data[(index * 3) + 1];
        }

        public int getZ(int index) {
            return this.data[(index * 3) + 2];
        }
    }
}
