package me.jellysquid.mods.sodium.client.world.biome;

import me.jellysquid.mods.sodium.client.world.WorldSlice;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.source.BiomeCoords;
import net.minecraft.world.biome.source.SeedMixer;

public class BiomeCache {
    private static final int SIZE = 3 * 4; // 3 chunks * 4 biomes per chunk

    // Arrays are in ZYX order
    private final Biome[] biomes = new Biome[SIZE * SIZE * SIZE];
    //Uniform looks at the own biome block and its 7 positive neighbors.
    //Index using the x and y coordinate. The z coordinate is used to access a single bit of each short.
    private final short[] uniform = new short[(SIZE-3) * (SIZE-3)];
    private final BiasMap bias = new BiasMap();

    private long biomeSeed;

    private int worldX, worldY, worldZ;

    public void update(WorldSlice slice) {
        var pos = slice.getOrigin();

        this.worldX = pos.getMinX() - 16;
        this.worldY = pos.getMinY() - 16;
        this.worldZ = pos.getMinZ() - 16;

        this.biomeSeed = slice.getBiomeSeed();

        this.copyBiomeData(slice);

        this.calculateBias();
        this.calculateUniform();
    }

    private void copyBiomeData(WorldSlice slice) {
        for (int sectionX = 0; sectionX < 3; sectionX++) {
            for (int sectionY = 0; sectionY < 3; sectionY++) {
                for (int sectionZ = 0; sectionZ < 3; sectionZ++) {
                    this.copySectionBiomeData(slice, sectionX, sectionY, sectionZ);
                }
            }
        }
    }

    private void copySectionBiomeData(WorldSlice slice, int sectionX, int sectionY, int sectionZ) {
        var section = slice.getSection(sectionX, sectionY, sectionZ);

        for (int x = 0; x < 4; x++) {
            for (int y = 0; y < 4; y++) {
                for (int z = 0; z < 4; z++) {
                    int biomeX = (sectionX * 4) + x;
                    int biomeY = (sectionY * 4) + y;
                    int biomeZ = (sectionZ * 4) + z;

                    this.biomes[dataArrayIndex(biomeX, biomeY, biomeZ)] = section.getBiome(x, y, z).value();
                }
            }
        }
    }

    private void calculateUniform() {
        for (int x = 1; x < (SIZE - 2); x++) {
            for (int y = 1; y < (SIZE - 2); y++) {
                int uniform = 0;
                for (int z = 1; z < (SIZE - 2); z++) {
                    uniform |= (this.hasUniformNeighbors(x, y, z) ? 1 : 0) << z;
                }
                this.uniform[uniformArrayIndex(x, y)] = (short) uniform;
            }
        }
    }

    private void calculateBias() {
        int offsetX = this.worldX >> 2;
        int offsetY = this.worldY >> 2;
        int offsetZ = this.worldZ >> 2;

        long seed = this.biomeSeed;

        for (int cellX = 1; cellX < 11; cellX++) {
            int worldCellX = offsetX + cellX;
            long seedX = SeedMixer.mixSeed(seed, worldCellX);

            for (int cellY = 1; cellY < 11; cellY++) {
                int worldCellY = offsetY + cellY;
                long seedXY = SeedMixer.mixSeed(seedX, worldCellY);

                for (int cellZ = 1; cellZ < 11; cellZ++) {
                    int worldCellZ = offsetZ + cellZ;
                    long seedXYZ = SeedMixer.mixSeed(seedXY, worldCellZ);

                    this.calculateBias(dataArrayIndex(cellX, cellY, cellZ),
                            worldCellX, worldCellY, worldCellZ, seedXYZ);
                }
            }
        }

    }

    private void calculateBias(int index, int x, int y, int z, long seed) {
        seed = SeedMixer.mixSeed(seed, x);
        seed = SeedMixer.mixSeed(seed, y);
        seed = SeedMixer.mixSeed(seed, z);

        var gradX = getBias(seed); seed = SeedMixer.mixSeed(seed, this.biomeSeed);
        var gradY = getBias(seed); seed = SeedMixer.mixSeed(seed, this.biomeSeed);
        var gradZ = getBias(seed);

        this.bias.set(index, gradX, gradY, gradZ);
    }

    private boolean hasUniformNeighbors(int x, int y, int z) {
        Biome biome = this.biomes[dataArrayIndex(x, y, z)];
        
        int maxX = x + 1;
        int maxY = y + 1;
        int maxZ = z + 1;

        for (int adjX = x; adjX <= maxX; adjX++) {
            for (int adjY = y; adjY <= maxY; adjY++) {
                for (int adjZ = z; adjZ <= maxZ; adjZ++) {
                    if (this.biomes[dataArrayIndex(adjX, adjY, adjZ)] != biome) {
                        return false;
                    }
                }
            }
        }

        return true;
    }

    public Biome getBiome(int x, int y, int z) {
        int biomeX = BiomeCoords.fromBlock(x - 2);
        int biomeY = BiomeCoords.fromBlock(y - 2);
        int biomeZ = BiomeCoords.fromBlock(z - 2);

        if ((this.uniform[uniformArrayIndex(biomeX, biomeY)] & (1 << biomeZ)) != 0) {
            return this.biomes[dataArrayIndex(biomeX, biomeY, biomeZ)];
        }

        return this.getBiomeUsingVoronoi(x, y, z);
    }

    private Biome getBiomeUsingVoronoi(int worldX, int worldY, int worldZ) {
        int x = worldX - 2;
        int y = worldY - 2;
        int z = worldZ - 2;

        int intX = BiomeCoords.fromBlock(x);
        int intY = BiomeCoords.fromBlock(y);
        int intZ = BiomeCoords.fromBlock(z);

        float fracX = BiomeCoords.method_39920(x) * 0.25f;
        float fracY = BiomeCoords.method_39920(y) * 0.25f;
        float fracZ = BiomeCoords.method_39920(z) * 0.25f;

        float closestDistance = Float.POSITIVE_INFINITY;
        int closestArrayIndex = 0;

        // Find the closest Voronoi cell to the given world coordinate
        // The distance is calculated between center positions, which are offset by the bias parameter
        // The bias is pre-computed and stored for each cell
        for (int index = 0; index < 8; index++) {
            boolean dirX = (index & 4) != 0;
            boolean dirY = (index & 2) != 0;
            boolean dirZ = (index & 1) != 0;

            int adjIntX = intX + (dirX ? 1 : 0);
            int adjIntY = intY + (dirY ? 1 : 0);
            int adjIntZ = intZ + (dirZ ? 1 : 0);

            float adjFracX = fracX - (dirX ? 1.0f : 0.0f);
            float adjFracY = fracY - (dirY ? 1.0f : 0.0f);
            float adjFracZ = fracZ - (dirZ ? 1.0f : 0.0f);

            int biasIndex = dataArrayIndex(adjIntX, adjIntY, adjIntZ);

            float biasX = biasToVector(this.bias.getX(biasIndex));
            float biasY = biasToVector(this.bias.getY(biasIndex));
            float biasZ = biasToVector(this.bias.getZ(biasIndex));

            float distanceX = MathHelper.square(adjFracX + biasX);
            float distanceY = MathHelper.square(adjFracY + biasY);
            float distanceZ = MathHelper.square(adjFracZ + biasZ);

            float distance = distanceX + distanceY + distanceZ;

            if (closestDistance > distance) {
                closestArrayIndex = biasIndex;
                closestDistance = distance;
            }
        }

        return this.biomes[closestArrayIndex];
    }

    private static int dataArrayIndex(int x, int y, int z) {
        return (x * SIZE * SIZE) + (y * SIZE) + z;
    }

    private static int uniformArrayIndex(int x, int y) {
        return ((x - 1) * (SIZE - 3)) + y - 1;
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
