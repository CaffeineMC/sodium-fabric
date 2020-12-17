package me.jellysquid.mods.sodium.client.world;

import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import me.jellysquid.mods.sodium.client.render.chunk.compile.ChunkBuilder;
import me.jellysquid.mods.sodium.client.world.biome.BiomeCache;
import me.jellysquid.mods.sodium.client.world.biome.BiomeCacheManager;
import me.jellysquid.mods.sodium.client.world.biome.BiomeColorCache;
import me.jellysquid.mods.sodium.common.util.pool.ReusableObject;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.util.collection.PackedIntegerArray;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.BlockRenderView;
import net.minecraft.world.LightType;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.source.BiomeAccess;
import net.minecraft.world.biome.source.BiomeArray;
import net.minecraft.world.chunk.*;
import net.minecraft.world.chunk.light.ChunkLightingView;
import net.minecraft.world.chunk.light.LightingProvider;
import net.minecraft.world.level.ColorResolver;

import java.util.Arrays;
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
public class WorldSlice extends ReusableObject implements BlockRenderView, BiomeAccess.Storage {
    private static final ChunkSection EMPTY_SECTION = new ChunkSection(0);

    // The number of blocks on each axis in a section.
    private static final int SECTION_BLOCK_LENGTH = 16;

    // The number of blocks in a section.
    private static final int SECTION_BLOCK_COUNT = SECTION_BLOCK_LENGTH * SECTION_BLOCK_LENGTH * SECTION_BLOCK_LENGTH;

    // The radius of blocks around the origin chunk that should be copied.
    private static final int NEIGHBOR_BLOCK_RADIUS = 1;

    // The radius of chunks around the origin chunk that should be copied.
    private static final int NEIGHBOR_CHUNK_RADIUS = MathHelper.roundUpToMultiple(NEIGHBOR_BLOCK_RADIUS, 16) >> 4;

    // The number of blocks on each axis of this slice.
    private static final int BLOCK_LENGTH = SECTION_BLOCK_LENGTH + (NEIGHBOR_BLOCK_RADIUS * 2);

    // The number of sections on each axis of this slice.
    private static final int SECTION_LENGTH = 1 + (NEIGHBOR_CHUNK_RADIUS * 2);

    // The size of the lookup tables used for mapping values to coordinate int pairs. The lookup table size is always
    // a power of two so that multiplications can be replaced with simple bit shifts in hot code paths.
    private static final int TABLE_LENGTH = MathHelper.smallestEncompassingPowerOfTwo(SECTION_LENGTH);

    // The number of bits needed for each X/Y/Z component in a lookup table.
    private static final int TABLE_BITS = Integer.bitCount(TABLE_LENGTH - 1);

    // The array size for the chunk lookup table.
    private static final int CHUNK_TABLE_ARRAY_SIZE = TABLE_LENGTH * TABLE_LENGTH;

    // The array size for the section lookup table.
    private static final int SECTION_TABLE_ARRAY_SIZE = TABLE_LENGTH * TABLE_LENGTH * TABLE_LENGTH;

    // Local Section->BlockState table. Read-only.
    private final BlockState[][] blockStatesArrays;

    // A pointer to the BlockState array for the origin section.
    private final BlockState[] originBlockStates;

    // Local Section->Light table. Read-only.
    private final ChunkNibbleArray[] blockLightArrays;
    private final ChunkNibbleArray[] skyLightArrays;

    // Local Section->Biome table.
    private final BiomeCache[] biomeCaches;
    private final BiomeArray[] biomeArrays;

    // The biome blend caches for each color resolver type
    // This map is always re-initialized, but the caches themselves are taken from an object pool
    private final Map<ColorResolver, BiomeColorCache> colorResolvers = new Reference2ObjectOpenHashMap<>();

    // The previously accessed and cached color resolver, used in conjunction with the cached color cache field
    private ColorResolver prevColorResolver;

    // The cached lookup result for the previously accessed color resolver to avoid excess hash table accesses
    // for vertex color blending
    private BiomeColorCache prevColorCache;

    // The world this slice has copied data from
    private World world;

    // Pointers to the chunks this slice encompasses
    private WorldChunk[] chunks;

    private BiomeCacheManager biomeCacheManager;

    // The starting point from which this slice captures blocks
    private int baseX, baseY, baseZ;

    // The min/max bounds of the blocks copied by this slice
    private int minX, minY, minZ;
    private int maxX, maxY, maxZ;

    // The chunk origin of this slice
    private ChunkSectionPos origin;

    public static WorldChunk[] createChunkSlice(World world, ChunkSectionPos pos) {
        WorldChunk chunk = world.getChunk(pos.getX(), pos.getZ());
        ChunkSection section = chunk.getSectionArray()[pos.getY()];

        // If the chunk section is absent or empty, simply terminate now. There will never be anything in this chunk
        // section to render, so we need to signal that a chunk render task shouldn't created. This saves a considerable
        // amount of time in queueing instant build tasks and greatly accelerates how quickly the world can be loaded.
        if (section == null || section.isEmpty()) {
            return null;
        }

        int minChunkX = pos.getX() - NEIGHBOR_CHUNK_RADIUS;
        int minChunkZ = pos.getZ() - NEIGHBOR_CHUNK_RADIUS;

        int maxChunkX = pos.getX() + NEIGHBOR_CHUNK_RADIUS;
        int maxChunkZ = pos.getZ() + NEIGHBOR_CHUNK_RADIUS;

        WorldChunk[] chunks = new WorldChunk[CHUNK_TABLE_ARRAY_SIZE];

        // Create an array of references to the world chunks in this slice
        for (int x = minChunkX; x <= maxChunkX; x++) {
            for (int z = minChunkZ; z <= maxChunkZ; z++) {
                chunks[getLocalChunkIndex(x - minChunkX, z - minChunkZ)] = world.getChunk(x, z);
            }
        }

        return chunks;
    }

    public WorldSlice() {
        this.blockStatesArrays = new BlockState[SECTION_TABLE_ARRAY_SIZE][];

        for (int x = 0; x < SECTION_LENGTH; x++) {
            for (int y = 0; y < SECTION_LENGTH; y++) {
                for (int z = 0; z < SECTION_LENGTH; z++) {
                    this.blockStatesArrays[getLocalSectionIndex(x, y, z)] = new BlockState[SECTION_BLOCK_COUNT];
                }
            }
        }

        this.blockLightArrays = new ChunkNibbleArray[SECTION_TABLE_ARRAY_SIZE];
        this.skyLightArrays = new ChunkNibbleArray[SECTION_TABLE_ARRAY_SIZE];

        this.biomeCaches = new BiomeCache[CHUNK_TABLE_ARRAY_SIZE];
        this.biomeArrays = new BiomeArray[CHUNK_TABLE_ARRAY_SIZE];

        this.originBlockStates = this.blockStatesArrays[getLocalSectionIndex((SECTION_LENGTH / 2), (SECTION_LENGTH / 2), (SECTION_LENGTH / 2))];
    }

    public void init(ChunkBuilder<?> builder, World world, ChunkSectionPos origin, WorldChunk[] chunks) {
        this.world = world;
        this.chunks = chunks;
        this.origin = origin;

        this.minX = origin.getMinX() - NEIGHBOR_BLOCK_RADIUS;
        this.minY = origin.getMinY() - NEIGHBOR_BLOCK_RADIUS;
        this.minZ = origin.getMinZ() - NEIGHBOR_BLOCK_RADIUS;

        this.maxX = origin.getMaxX() + NEIGHBOR_BLOCK_RADIUS;
        this.maxY = origin.getMaxY() + NEIGHBOR_BLOCK_RADIUS;
        this.maxZ = origin.getMaxZ() + NEIGHBOR_BLOCK_RADIUS;

        final int minChunkX = this.minX >> 4;
        final int minChunkY = this.minY >> 4;
        final int minChunkZ = this.minZ >> 4;

        final int maxChunkX = this.maxX >> 4;
        final int maxChunkY = this.maxY >> 4;
        final int maxChunkZ = this.maxZ >> 4;

        this.baseX = minChunkX << 4;
        this.baseY = minChunkY << 4;
        this.baseZ = minChunkZ << 4;

        // Iterate over all sliced chunks
        for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
            for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                // The local index for this chunk in the slice's data arrays
                int chunkIdx = getLocalChunkIndex(chunkX - minChunkX, chunkZ - minChunkZ);

                Chunk chunk = this.chunks[chunkIdx];

                for (int chunkY = minChunkY; chunkY <= maxChunkY; chunkY++) {
                    ChunkSectionPos pos = ChunkSectionPos.from(chunkX, chunkY, chunkZ);

                    // Find the local position of the chunk in the sliced chunk array
                    int sectionIdx = getLocalSectionIndex(chunkX - minChunkX, chunkY - minChunkY, chunkZ - minChunkZ);

                    this.populateLightArrays(sectionIdx, pos);
                    this.populateBlockArrays(sectionIdx, pos, chunk);
                }

                this.biomeArrays[chunkIdx] = chunk.getBiomeArray();
            }
        }

        this.biomeCacheManager = builder.getBiomeCacheManager();
        this.biomeCacheManager.populateArrays(origin.getX(), origin.getY(), origin.getZ(), this.biomeCaches);
    }

    private void populateLightArrays(int sectionIdx, ChunkSectionPos pos) {
        if (World.isHeightInvalid(pos.getY())) {
            return;
        }

        ChunkLightingView blockLightProvider = this.world.getLightingProvider().get(LightType.BLOCK);
        ChunkLightingView skyLightProvider = this.world.getLightingProvider().get(LightType.SKY);
        
        this.blockLightArrays[sectionIdx] = blockLightProvider.getLightSection(pos);
        this.skyLightArrays[sectionIdx] = skyLightProvider.getLightSection(pos);
    }

    private void populateBlockArrays(int sectionIdx, ChunkSectionPos pos, Chunk chunk) {
        ChunkSection section = getChunkSection(chunk, pos);

        if (section == null || section.isEmpty()) {
            section = EMPTY_SECTION;
        }

        PalettedContainer<BlockState> container = section.getContainer();

        PackedIntegerArray intArray = container.data;
        Palette<BlockState> palette = container.palette;

        BlockState[] dst = this.blockStatesArrays[sectionIdx];

        int minBlockX = Math.max(this.minX, pos.getMinX());
        int maxBlockX = Math.min(this.maxX, pos.getMaxX());

        int minBlockY = Math.max(this.minY, pos.getMinY());
        int maxBlockY = Math.min(this.maxY, pos.getMaxY());

        int minBlockZ = Math.max(this.minZ, pos.getMinZ());
        int maxBlockZ = Math.min(this.maxZ, pos.getMaxZ());

        int prevPaletteId = -1;
        BlockState prevPaletteState = null;

        for (int y = minBlockY; y <= maxBlockY; y++) {
            for (int z = minBlockZ; z <= maxBlockZ; z++) {
                for (int x = minBlockX; x <= maxBlockX; x++) {
                    int blockIdx = getLocalBlockIndex(x & 15, y & 15, z & 15);
                    int paletteId = intArray.get(blockIdx);

                    BlockState state;

                    if (prevPaletteId == paletteId) {
                        state = prevPaletteState;
                    } else {
                        state = palette.getByIndex(paletteId);

                        prevPaletteState = state;
                        prevPaletteId = paletteId;
                    }

                    dst[blockIdx] = state;
                }
            }
        }
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

    public BlockState getOriginBlockState(int x, int y, int z) {
        return this.originBlockStates[getLocalBlockIndex(x, y, z)];
    }

    @Override
    public FluidState getFluidState(BlockPos pos) {
        return this.getBlockState(pos)
                .getFluidState();
    }

    public FluidState getFluidState(int x, int y, int z) {
        return this.getBlockState(x, y, z)
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
        return this.getBlockEntity(pos, WorldChunk.CreationType.IMMEDIATE);
    }

    public BlockEntity getBlockEntity(BlockPos pos, WorldChunk.CreationType type) {
        int relX = pos.getX() - this.baseX;
        int relZ = pos.getZ() - this.baseZ;

        return this.chunks[getLocalChunkIndex(relX >> 4, relZ >> 4)]
                .getBlockEntity(pos, type);
    }

    @Override
    public int getColor(BlockPos pos, ColorResolver resolver) {
        BiomeColorCache cache;

        if (this.prevColorResolver == resolver) {
            cache = this.prevColorCache;
        } else {
            cache = this.colorResolvers.get(resolver);

            if (cache == null) {
                this.colorResolvers.put(resolver, cache = new BiomeColorCache(resolver, this));
            }

            this.prevColorResolver = resolver;
            this.prevColorCache = cache;
        }

        return cache.getBlendedColor(pos);
    }

    @Override
    public int getLightLevel(LightType type, BlockPos pos) {
        switch (type) {
            case SKY:
                return this.getLightLevel(this.skyLightArrays, pos);
            case BLOCK:
                return this.getLightLevel(this.blockLightArrays, pos);
            default:
                return 0;
        }
    }

    @Override
    public int getBaseLightLevel(BlockPos pos, int ambientDarkness) {
        return 0;
    }

    @Override
    public boolean isSkyVisible(BlockPos pos) {
        return false;
    }

    private int getLightLevel(ChunkNibbleArray[] arrays, BlockPos pos) {
        int relX = pos.getX() - this.baseX;
        int relY = pos.getY() - this.baseY;
        int relZ = pos.getZ() - this.baseZ;

        ChunkNibbleArray array = arrays[getLocalSectionIndex(relX >> 4, relY >> 4, relZ >> 4)];

        if (array != null) {
            return array.get(relX & 15, relY & 15, relZ & 15);
        }

        return 0;
    }

    // TODO: Is this safe? The biome data arrays should be immutable once loaded into the client
    @Override
    public Biome getBiomeForNoiseGen(int x, int y, int z) {
        int x2 = (x >> 2) - (this.baseX >> 4);
        int z2 = (z >> 2) - (this.baseZ >> 4);

        // Coordinates are in biome space!
        // [VanillaCopy] WorldView#getBiomeForNoiseGen(int, int, int)
        BiomeArray array = this.biomeArrays[getLocalChunkIndex(x2, z2)];

        if (array != null ) {
            return array.getBiomeForNoiseGen(x, y, z);
        }

        return this.world.getGeneratorStoredBiome(x, y, z);
    }

    /**
     * Gets or computes the biome at the given global coordinates.
     */
    public Biome getCachedBiome(int x, int z) {
        int relX = x - this.baseX;
        int relZ = z - this.baseZ;

        return this.biomeCaches[getLocalChunkIndex(relX >> 4, relZ >> 4)]
                .getBiome(this, x, z);
    }

    public ChunkSectionPos getOrigin() {
        return this.origin;
    }

    @Override
    public void reset() {
        for (BiomeCache cache : this.biomeCaches) {
            if (cache != null) {
                this.biomeCacheManager.release(cache);
            }
        }

        Arrays.fill(this.biomeCaches, null);
        Arrays.fill(this.biomeArrays, null);
        Arrays.fill(this.blockLightArrays, null);
        Arrays.fill(this.skyLightArrays, null);

        this.biomeCacheManager = null;
        this.chunks = null;
        this.world = null;

        this.colorResolvers.clear();
        this.prevColorCache = null;
        this.prevColorResolver = null;
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

    private static ChunkSection getChunkSection(Chunk chunk, ChunkSectionPos pos) {
        ChunkSection section = null;

        if (!World.isHeightInvalid(pos.getY())) {
            section = chunk.getSectionArray()[pos.getY()];
        }

        return section;
    }
}
