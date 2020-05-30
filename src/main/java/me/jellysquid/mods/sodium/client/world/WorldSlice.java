package me.jellysquid.mods.sodium.client.world;

import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import me.jellysquid.mods.sodium.client.render.chunk.compile.ChunkBuilder;
import me.jellysquid.mods.sodium.client.world.biome.BiomeCache;
import me.jellysquid.mods.sodium.client.world.biome.BiomeCacheManager;
import me.jellysquid.mods.sodium.client.world.biome.BiomeColorBlendCache;
import me.jellysquid.mods.sodium.common.util.pool.ReusableObject;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.BlockRenderView;
import net.minecraft.world.LightType;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.source.BiomeAccess;
import net.minecraft.world.biome.source.BiomeArray;
import net.minecraft.world.chunk.ChunkNibbleArray;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.WorldChunk;
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
 * You should use object pooling with this type to avoid huge allocations as instances of this class contain many large
 * arrays.
 */
public class WorldSlice extends ReusableObject implements BlockRenderView, BiomeAccess.Storage {
    // The number of outward blocks from the origin chunk to slice
    public static final int NEIGHBOR_BLOCK_RADIUS = 1;

    // The number of outward chunks from the origin chunk to slice
    public static final int NEIGHBOR_CHUNK_RADIUS = MathHelper.roundUp(NEIGHBOR_BLOCK_RADIUS, 16) >> 4;

    // The length of the chunk section array on each axis
    public static final int SECTION_LENGTH = 1 + (NEIGHBOR_CHUNK_RADIUS * 2);

    // The number of blocks
    public static final int BLOCK_LENGTH = 16 + (NEIGHBOR_BLOCK_RADIUS * 2);

    // The number of blocks contained by a world slice
    public static final int BLOCK_COUNT = BLOCK_LENGTH * BLOCK_LENGTH * BLOCK_LENGTH;

    // The number of chunk sections contained by a world slice
    public static final int SECTION_COUNT = SECTION_LENGTH * SECTION_LENGTH * SECTION_LENGTH;

    // The number of chunks contained by a world slice
    public static final int CHUNK_COUNT = SECTION_LENGTH * SECTION_LENGTH;

    private static final ChunkSection EMPTY_SECTION = new ChunkSection(0);

    // The data arrays for this slice
    // These are allocated once and then re-used when the slice is released back to an object pool
    private final BlockState[] blockStates;
    private final ChunkNibbleArray[] blockLightArrays;
    private final ChunkNibbleArray[] skyLightArrays;
    private final BiomeCache[] biomeCaches;
    private final BiomeArray[] biomeArrays;

    // The biome blend caches for each color resolver type
    // This map is always re-initialized, but the caches themselves are taken from an object pool
    private final Map<ColorResolver, BiomeColorBlendCache> colorResolvers = new Reference2ObjectOpenHashMap<>();

    // The previously accessed and cached color resolver, used in conjunction with the cached color cache field
    private ColorResolver prevColorResolver;

    // The cached lookup result for the previously accessed color resolver to avoid excess hash table accesses
    // for vertex color blending
    private BiomeColorBlendCache prevColorCache;

    // The world this slice has copied data from, not thread-safe
    private World world;

    // Pointers to the chunks this slice encompasses, not thread-safe
    private WorldChunk[] chunks;

    private BiomeCacheManager biomeCacheManager;

    // The starting point from which this slice captures chunks
    private int chunkOffsetX, chunkOffsetY, chunkOffsetZ;

    // The starting point from which this slice captures blocks
    private int blockOffsetX, blockOffsetY, blockOffsetZ;

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

        WorldChunk[] chunks = new WorldChunk[SECTION_LENGTH * SECTION_LENGTH];

        // Create an array of references to the world chunks in this slice
        for (int x = minChunkX; x <= maxChunkX; x++) {
            for (int z = minChunkZ; z <= maxChunkZ; z++) {
                chunks[getLocalChunkIndex(x - minChunkX, z - minChunkZ)] = world.getChunk(x, z);
            }
        }

        return chunks;
    }

    public WorldSlice() {
        this.blockStates = new BlockState[BLOCK_COUNT];
        this.blockLightArrays = new ChunkNibbleArray[SECTION_COUNT];
        this.skyLightArrays = new ChunkNibbleArray[SECTION_COUNT];
        this.biomeCaches = new BiomeCache[CHUNK_COUNT];
        this.biomeArrays = new BiomeArray[CHUNK_COUNT];
    }

    public void init(ChunkBuilder<?> builder, World world, ChunkSectionPos chunkPos, WorldChunk[] chunks) {
        final int minX = chunkPos.getMinX() - NEIGHBOR_BLOCK_RADIUS;
        final int minY = chunkPos.getMinY() - NEIGHBOR_BLOCK_RADIUS;
        final int minZ = chunkPos.getMinZ() - NEIGHBOR_BLOCK_RADIUS;

        final int maxX = chunkPos.getMaxX() + NEIGHBOR_BLOCK_RADIUS + 1;
        final int maxY = chunkPos.getMaxY() + NEIGHBOR_BLOCK_RADIUS + 1;
        final int maxZ = chunkPos.getMaxZ() + NEIGHBOR_BLOCK_RADIUS + 1;

        final int minChunkX = minX >> 4;
        final int maxChunkX = maxX >> 4;

        final int minChunkY = minY >> 4;
        final int maxChunkY = maxY >> 4;

        final int minChunkZ = minZ >> 4;
        final int maxChunkZ = maxZ >> 4;

        this.world = world;
        this.chunks = chunks;

        this.blockOffsetX = minX;
        this.blockOffsetY = minY;
        this.blockOffsetZ = minZ;

        this.chunkOffsetX = chunkPos.getX() - NEIGHBOR_CHUNK_RADIUS;
        this.chunkOffsetY = chunkPos.getY() - NEIGHBOR_CHUNK_RADIUS;
        this.chunkOffsetZ = chunkPos.getZ() - NEIGHBOR_CHUNK_RADIUS;

        // Hoist the lighting providers so that they can be directly accessed
        ChunkLightingView blockLightProvider = this.world.getLightingProvider().get(LightType.BLOCK);
        ChunkLightingView skyLightProvider = this.world.getLightingProvider().get(LightType.SKY);

        // Iterate over all sliced chunks
        for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
            for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                // Find the local position of the chunk in the sliced chunk array
                int chunkXLocal = chunkX - this.chunkOffsetX;
                int chunkZLocal = chunkZ - this.chunkOffsetZ;

                // The local index for this chunk in the slice's data arrays
                int chunkIdx = getLocalChunkIndex(chunkXLocal, chunkZLocal);

                WorldChunk chunk = chunks[chunkIdx];

                this.biomeArrays[chunkIdx] = chunk.getBiomeArray();

                int minBlockX = Math.max(minX, chunkX << 4);
                int maxBlockX = Math.min(maxX, (chunkX + 1) << 4);

                int minBlockZ = Math.max(minZ, chunkZ << 4);
                int maxBlockZ = Math.min(maxZ, (chunkZ + 1) << 4);

                for (int chunkY = minChunkY; chunkY <= maxChunkY; chunkY++) {
                    int chunkYLocal = chunkY - this.chunkOffsetY;

                    ChunkSectionPos sectionPos = ChunkSectionPos.from(chunkX, chunkY, chunkZ);
                    int sectionIdx = getLocalSectionIndex(chunkXLocal, chunkYLocal, chunkZLocal);

                    this.blockLightArrays[sectionIdx] = blockLightProvider.getLightArray(sectionPos);
                    this.skyLightArrays[sectionIdx] = skyLightProvider.getLightArray(sectionPos);

                    ChunkSection section = null;

                    // Fetch the chunk section for this position if it's within bounds
                    if (chunkY >= 0 && chunkY < 16) {
                        section = chunk.getSectionArray()[chunkY];
                    }

                    // If no chunk section has been fetched, use an empty one which will return air blocks in the copy below
                    if (section == null) {
                        section = EMPTY_SECTION;
                    }

                    int minBlockY = Math.max(minY, chunkY << 4);
                    int maxBlockY = Math.min(maxY, (chunkY + 1) << 4);

                    // Iterate over all block states in the overlapping section between this world slice and chunk section
                    for (int y = minBlockY; y < maxBlockY; y++) {
                        for (int z = minBlockZ; z < maxBlockZ; z++) {
                            for (int x = minBlockX; x < maxBlockX; x++) {
                                this.blockStates[this.getBlockIndex(x, y, z)] = section.getBlockState(x & 15, y & 15, z & 15);
                            }
                        }
                    }
                }
            }
        }

        this.biomeCacheManager = builder.getBiomeCacheManager();
        this.biomeCacheManager.populateArrays(chunkPos.getX(), chunkPos.getY(), chunkPos.getZ(), this.biomeCaches);
    }

    @Override
    public BlockState getBlockState(BlockPos pos) {
        return this.blockStates[this.getBlockIndex(pos.getX(), pos.getY(), pos.getZ())];
    }

    public BlockState getBlockState(int x, int y, int z) {
        return this.blockStates[this.getBlockIndex(x, y, z)];
    }

    @Override
    public FluidState getFluidState(BlockPos pos) {
        return this.getBlockState(pos).getFluidState();
    }

    public FluidState getFluidState(int x, int y, int z) {
        return this.getBlockState(x, y, z).getFluidState();
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
        return this.chunks[this.getChunkIndexForBlock(pos)].getBlockEntity(pos, type);
    }

    @Override
    public int getColor(BlockPos pos, ColorResolver resolver) {
        BiomeColorBlendCache cache;

        if (this.prevColorResolver == resolver) {
            cache = this.prevColorCache;
        } else {
            cache = this.colorResolvers.get(resolver);

            if (cache == null) {
                this.colorResolvers.put(resolver, cache = new BiomeColorBlendCache(resolver, this));
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

    private int getLightLevel(ChunkNibbleArray[] arrays, BlockPos pos) {
        int x = pos.getX();
        int y = pos.getY();
        int z = pos.getZ();

        ChunkNibbleArray array = arrays[this.getSectionIndexForBlock(x, y, z)];

        if (array != null) {
            return array.get(x & 15, y & 15, z & 15);
        }

        return 0;
    }

    // TODO: Is this safe? The biome data arrays should be immutable once loaded into the client
    @Override
    public Biome getBiomeForNoiseGen(int x, int y, int z) {
        BiomeArray array = this.biomeArrays[this.getBiomeIndexForBlock(x, z)];

        if (array != null ) {
            return array.getBiomeForNoiseGen(x, y, z);
        }

        return this.world.getGeneratorStoredBiome(x, y, z);
    }

    /**
     * Gets or computes the biome at the given global coordinates.
     */
    public Biome getCachedBiome(int x, int z) {
        return this.biomeCaches[this.getChunkIndexForBlock(x, z)].getBiome(this, x, z);
    }

    /**
     * Returns the index of a block in global coordinate space for this slice.
     */
    private int getBlockIndex(int x, int y, int z) {
        int x2 = x - this.blockOffsetX;
        int y2 = y - this.blockOffsetY;
        int z2 = z - this.blockOffsetZ;

        return (y2 * BLOCK_LENGTH * BLOCK_LENGTH) + (z2 * BLOCK_LENGTH) + x2;
    }

    /**
     * {@link WorldSlice#getChunkIndexForBlock(int, int)}
     */
    private int getChunkIndexForBlock(BlockPos pos) {
        return this.getChunkIndexForBlock(pos.getX(), pos.getZ());
    }

    /**
     * Returns the index of a chunk in global coordinate space for this slice.
     */
    private int getChunkIndexForBlock(int x, int z) {
        int x2 = (x >> 4) - this.chunkOffsetX;
        int z2 = (z >> 4) - this.chunkOffsetZ;

        return getLocalChunkIndex(x2, z2);
    }

    /**
     * Returns the index of a biome in the global coordinate space for this slice.
     */
    private int getBiomeIndexForBlock(int x, int z) {
        // Coordinates are in biome space!
        // [VanillaCopy] WorldView#getBiomeForNoiseGen(int, int, int)
        int x2 = (x >> 2) - this.chunkOffsetX;
        int z2 = (z >> 2) - this.chunkOffsetZ;

        return getLocalChunkIndex(x2, z2);
    }

    /**
     * Returns the index of a chunk section in global coordinate space for this slice.
     */
    private int getSectionIndexForBlock(int x, int y, int z) {
        int x2 = (x >> 4) - this.chunkOffsetX;
        int y2 = (y >> 4) - this.chunkOffsetY;
        int z2 = (z >> 4) - this.chunkOffsetZ;

        return getLocalSectionIndex(x2, y2, z2);
    }

    /**
     * Returns the index of a chunk in local coordinate space to this slice.
     */
    public static int getLocalChunkIndex(int x, int z) {
        return (z * SECTION_LENGTH) + x;
    }

    /**
     * Returns the index of a chunk section in local coordinate space to this slice.
     */
    public static int getLocalSectionIndex(int x, int y, int z) {
        return (y * SECTION_LENGTH * SECTION_LENGTH) + (z * SECTION_LENGTH) + x;
    }

    @Override
    public void reset() {
        for (BiomeCache cache : this.biomeCaches) {
            this.biomeCacheManager.release(cache);
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

    public int getBlockOffsetX() {
        return this.blockOffsetX;
    }

    public int getBlockOffsetY() {
        return this.blockOffsetY;
    }

    public int getBlockOffsetZ() {
        return this.blockOffsetZ;
    }
}
