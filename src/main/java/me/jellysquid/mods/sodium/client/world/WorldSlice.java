package me.jellysquid.mods.sodium.client.world;

import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import me.jellysquid.mods.sodium.client.world.cloned.PackedIntegerArrayExtended;
import me.jellysquid.mods.sodium.client.world.biome.BiomeCache;
import me.jellysquid.mods.sodium.client.world.biome.BiomeColorCache;
import me.jellysquid.mods.sodium.client.world.cloned.ChunkRenderContext;
import me.jellysquid.mods.sodium.client.world.cloned.ClonedChunkSection;
import me.jellysquid.mods.sodium.client.world.cloned.ClonedChunkSectionCache;
import me.jellysquid.mods.sodium.client.world.cloned.palette.ClonedPalette;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.util.collection.PackedIntegerArray;
import net.minecraft.util.math.*;
import net.minecraft.world.BlockRenderView;
import net.minecraft.world.LightType;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.source.BiomeAccess;
import net.minecraft.world.chunk.*;
import net.minecraft.world.chunk.light.LightingProvider;
import net.minecraft.world.level.ColorResolver;

import java.util.Map;

/**
 * Takes a slice of world state (block states, biome and light data arrays) and copies the data for use in off-thread
 * operations. This allows chunk build tasks to see a consistent snapshot of chunk data at the exact moment the task was
 * created.
 *
 * World slices are not safe to use from multiple threads at once, but the data they contain is safe from modification
 * by the main client thread.
 *
 * Object pooling should be used to avoid huge allocations as this class contains many large arrays.
 */
public class WorldSlice implements BlockRenderView, BiomeAccess.Storage {
    // The number of blocks on each axis in a section.
    private static final int SECTION_BLOCK_LENGTH = 16;

    // The number of blocks in a section.
    private static final int SECTION_BLOCK_COUNT = SECTION_BLOCK_LENGTH * SECTION_BLOCK_LENGTH * SECTION_BLOCK_LENGTH;

    // The radius of blocks around the origin chunk that should be copied.
    private static final int NEIGHBOR_BLOCK_RADIUS = 2;

    // The radius of chunks around the origin chunk that should be copied.
    private static final int NEIGHBOR_CHUNK_RADIUS = MathHelper.roundUpToMultiple(NEIGHBOR_BLOCK_RADIUS, 16) >> 4;

    // The number of sections on each axis of this slice.
    private static final int SECTION_LENGTH = 1 + (NEIGHBOR_CHUNK_RADIUS * 2);

    // The size of the lookup tables used for mapping values to coordinate int pairs. The lookup table size is always
    // a power of two so that multiplications can be replaced with simple bit shifts in hot code paths.
    private static final int TABLE_LENGTH = MathHelper.smallestEncompassingPowerOfTwo(SECTION_LENGTH);

    // The number of bits needed for each X/Y/Z component in a lookup table.
    private static final int TABLE_BITS = Integer.bitCount(TABLE_LENGTH - 1);

    // The array size for the section lookup table.
    private static final int SECTION_TABLE_ARRAY_SIZE = TABLE_LENGTH * TABLE_LENGTH * TABLE_LENGTH;

    // The world this slice has copied data from
    private final World world;

    // Local Section->BlockState table.
    private final BlockState[][] blockStatesArrays;

    // Local section copies. Read-only.
    private ClonedChunkSection[] sections;

    // Biome caches for each chunk section
    private BiomeCache[] biomeCaches;

    // The biome blend caches for each color resolver type
    // This map is always re-initialized, but the caches themselves are taken from an object pool
    private final Map<ColorResolver, BiomeColorCache> biomeColorCaches = new Reference2ObjectOpenHashMap<>();

    // The previously accessed and cached color resolver, used in conjunction with the cached color cache field
    private ColorResolver prevColorResolver;

    // The cached lookup result for the previously accessed color resolver to avoid excess hash table accesses
    // for vertex color blending
    private BiomeColorCache prevColorCache;

    // The starting point from which this slice captures blocks
    private int baseX, baseY, baseZ;

    // The chunk origin of this slice
    private ChunkSectionPos origin;

    public static ChunkRenderContext prepare(World world, ChunkSectionPos origin, ClonedChunkSectionCache sectionCache) {
        WorldChunk chunk = world.getChunk(origin.getX(), origin.getZ());
        ChunkSection section = chunk.getSectionArray()[origin.getY()];

        // If the chunk section is absent or empty, simply terminate now. There will never be anything in this chunk
        // section to render, so we need to signal that a chunk render task shouldn't created. This saves a considerable
        // amount of time in queueing instant build tasks and greatly accelerates how quickly the world can be loaded.
        if (ChunkSection.isEmpty(section)) {
            return null;
        }

        BlockBox volume = new BlockBox(origin.getMinX() - NEIGHBOR_BLOCK_RADIUS,
                origin.getMinY() - NEIGHBOR_BLOCK_RADIUS,
                origin.getMinZ() - NEIGHBOR_BLOCK_RADIUS,
                origin.getMaxX() + NEIGHBOR_BLOCK_RADIUS,
                origin.getMaxY() + NEIGHBOR_BLOCK_RADIUS,
                origin.getMaxZ() + NEIGHBOR_BLOCK_RADIUS);

        // The min/max bounds of the chunks copied by this slice
        final int minChunkX = origin.getX() - NEIGHBOR_CHUNK_RADIUS;
        final int minChunkY = origin.getY() - NEIGHBOR_CHUNK_RADIUS;
        final int minChunkZ = origin.getZ() - NEIGHBOR_CHUNK_RADIUS;

        final int maxChunkX = origin.getX() + NEIGHBOR_CHUNK_RADIUS;
        final int maxChunkY = origin.getY() + NEIGHBOR_CHUNK_RADIUS;
        final int maxChunkZ = origin.getZ() + NEIGHBOR_CHUNK_RADIUS;

        ClonedChunkSection[] sections = new ClonedChunkSection[SECTION_TABLE_ARRAY_SIZE];

        for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
            for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                for (int chunkY = minChunkY; chunkY <= maxChunkY; chunkY++) {
                    sections[getLocalSectionIndex(chunkX - minChunkX, chunkY - minChunkY, chunkZ - minChunkZ)] =
                            sectionCache.acquire(chunkX, chunkY, chunkZ);
                }
            }
        }

        return new ChunkRenderContext(origin, sections, volume);
    }

    public WorldSlice(World world) {
        this.world = world;

        this.sections = new ClonedChunkSection[SECTION_TABLE_ARRAY_SIZE];
        this.blockStatesArrays = new BlockState[SECTION_TABLE_ARRAY_SIZE][];
        this.biomeCaches = new BiomeCache[SECTION_TABLE_ARRAY_SIZE];

        for (int x = 0; x < SECTION_LENGTH; x++) {
            for (int y = 0; y < SECTION_LENGTH; y++) {
                for (int z = 0; z < SECTION_LENGTH; z++) {
                    int i = getLocalSectionIndex(x, y, z);

                    this.blockStatesArrays[i] = new BlockState[SECTION_BLOCK_COUNT];
                    this.biomeCaches[i] = new BiomeCache(this.world);
                }
            }
        }
    }

    public void copyData(ChunkRenderContext context) {
        this.origin = context.getOrigin();
        this.sections = context.getSections();

        this.prevColorCache = null;
        this.prevColorResolver = null;

        this.biomeColorCaches.clear();

        this.baseX = (this.origin.getX() - NEIGHBOR_CHUNK_RADIUS) << 4;
        this.baseY = (this.origin.getY() - NEIGHBOR_CHUNK_RADIUS) << 4;
        this.baseZ = (this.origin.getZ() - NEIGHBOR_CHUNK_RADIUS) << 4;

        for (int x = 0; x < SECTION_LENGTH; x++) {
            for (int y = 0; y < SECTION_LENGTH; y++) {
                for (int z = 0; z < SECTION_LENGTH; z++) {
                    int idx = getLocalSectionIndex(x, y, z);

                    this.biomeCaches[idx].reset();

                    this.unpackBlockData(this.blockStatesArrays[idx], this.sections[idx], context.getVolume());
                }
            }
        }
    }

    private void unpackBlockData(BlockState[] states, ClonedChunkSection section, BlockBox box) {
        if (this.origin.equals(section.getPosition()))  {
            this.unpackBlockDataZ(states, section);
        } else {
            this.unpackBlockDataR(states, section, box);
        }
    }

    private void unpackBlockDataR(BlockState[] states, ClonedChunkSection section, BlockBox box) {
        PackedIntegerArray intArray = section.getBlockData();
        ClonedPalette<BlockState> palette = section.getBlockPalette();

        ChunkSectionPos pos = section.getPosition();

        int minBlockX = Math.max(box.minX, pos.getMinX());
        int maxBlockX = Math.min(box.maxX, pos.getMaxX());

        int minBlockY = Math.max(box.minY, pos.getMinY());
        int maxBlockY = Math.min(box.maxY, pos.getMaxY());

        int minBlockZ = Math.max(box.minZ, pos.getMinZ());
        int maxBlockZ = Math.min(box.maxZ, pos.getMaxZ());

        for (int y = minBlockY; y <= maxBlockY; y++) {
            for (int z = minBlockZ; z <= maxBlockZ; z++) {
                for (int x = minBlockX; x <= maxBlockX; x++) {
                    int blockIdx = getLocalBlockIndex(x & 15, y & 15, z & 15);
                    int value = intArray.get(blockIdx);

                    states[blockIdx] = palette.get(value);
                }
            }
        }
    }

    private void unpackBlockDataZ(BlockState[] states, ClonedChunkSection section) {
        ((PackedIntegerArrayExtended) section.getBlockData())
                .copyUsingPalette(states, section.getBlockPalette());
    }

    @Override
    public BlockState getBlockState(BlockPos pos) {
        return this.getBlockState(pos.getX(), pos.getY(), pos.getZ());
    }

    public BlockState getBlockState(int x, int y, int z) {
        int relX = x - this.baseX;
        int relY = y - this.baseY;
        int relZ = z - this.baseZ;

        return this.blockStatesArrays[getLocalSectionIndex(relX >> 4, relY >> 4, relZ >> 4)]
                [getLocalBlockIndex(relX & 15, relY & 15, relZ & 15)];
    }

    public BlockState getBlockStateRelative(int x, int y, int z) {
        return this.blockStatesArrays[getLocalSectionIndex(x >> 4, y >> 4, z >> 4)]
                [getLocalBlockIndex(x & 15, y & 15, z & 15)];
    }

    @Override
    public FluidState getFluidState(BlockPos pos) {
        return this.getBlockState(pos)
                .getFluidState();
    }

    @Override
    public float getBrightness(Direction direction, boolean shaded) {
        return this.world.getBrightness(direction, shaded);
    }

    @Override
    public LightingProvider getLightingProvider() {
        return this.world.getLightingProvider();
    }

    @Override
    public BlockEntity getBlockEntity(BlockPos pos) {
        return this.getBlockEntity(pos.getX(), pos.getY(), pos.getZ());
    }

    public BlockEntity getBlockEntity(int x, int y, int z) {
        int relX = x - this.baseX;
        int relY = y - this.baseY;
        int relZ = z - this.baseZ;

        return this.sections[getLocalSectionIndex(relX >> 4, relY >> 4, relZ >> 4)]
                .getBlockEntity(relX & 15, relY & 15, relZ & 15);
    }

    @Override
    public int getColor(BlockPos pos, ColorResolver resolver) {
        BiomeColorCache cache;

        if (this.prevColorResolver == resolver) {
            cache = this.prevColorCache;
        } else {
            cache = this.biomeColorCaches.get(resolver);

            if (cache == null) {
                this.biomeColorCaches.put(resolver, cache = new BiomeColorCache(resolver, this));
            }

            this.prevColorResolver = resolver;
            this.prevColorCache = cache;
        }

        return cache.getBlendedColor(pos);
    }

    @Override
    public int getLightLevel(LightType type, BlockPos pos) {
        int relX = pos.getX() - this.baseX;
        int relY = pos.getY() - this.baseY;
        int relZ = pos.getZ() - this.baseZ;

        return this.sections[getLocalSectionIndex(relX >> 4, relY >> 4, relZ >> 4)]
                .getLightLevel(type, relX & 15, relY & 15, relZ & 15);
    }

    @Override
    public int getBaseLightLevel(BlockPos pos, int ambientDarkness) {
        return 0;
    }

    @Override
    public boolean isSkyVisible(BlockPos pos) {
        return false;
    }

    @Override
    public Biome getBiomeForNoiseGen(int x, int y, int z) {
        int x2 = (x >> 2) - (this.baseX >> 4);
        int z2 = (z >> 2) - (this.baseZ >> 4);

        // Coordinates are in biome space!
        // [VanillaCopy] WorldView#getBiomeForNoiseGen(int, int, int)
        ClonedChunkSection section = this.sections[getLocalChunkIndex(x2, z2)];

        if (section != null ) {
            return section.getBiomeForNoiseGen(x, y, z);
        }

        return this.world.getGeneratorStoredBiome(x, y, z);
    }

    /**
     * Gets or computes the biome at the given global coordinates.
     */
    public Biome getBiome(int x, int y, int z) {
        int relX = x - this.baseX;
        int relY = y - this.baseY;
        int relZ = z - this.baseZ;

        return this.biomeCaches[getLocalSectionIndex(relX >> 4, relY >> 4, relZ >> 4)]
                .getBiome(this, x, relY >> 4, z);
    }

    public ChunkSectionPos getOrigin() {
        return this.origin;
    }

    // [VanillaCopy] PalettedContainer#toIndex
    public static int getLocalBlockIndex(int x, int y, int z) {
        return y << 8 | z << 4 | x;
    }

    public static int getLocalSectionIndex(int x, int y, int z) {
        return y << TABLE_BITS << TABLE_BITS | z << TABLE_BITS | x;
    }

    public static int getLocalChunkIndex(int x, int z) {
        return z << TABLE_BITS | x;
    }
}
