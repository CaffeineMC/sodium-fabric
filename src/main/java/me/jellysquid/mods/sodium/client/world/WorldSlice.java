package me.jellysquid.mods.sodium.client.world;

import it.unimi.dsi.fastutil.objects.Reference2ReferenceArrayMap;
import me.jellysquid.mods.sodium.client.render.chunk.compile.ChunkBuilder;
import me.jellysquid.mods.sodium.client.world.biome.BiomeCache;
import me.jellysquid.mods.sodium.client.world.biome.BiomeCacheManager;
import me.jellysquid.mods.sodium.client.world.biome.ColorizerCache;
import me.jellysquid.mods.sodium.common.util.arena.ReusableObject;
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

public class WorldSlice extends ReusableObject implements BlockRenderView, BiomeAccess.Storage {
    public static final int BLOCK_RADIUS = 1;
    public static final int CHUNK_RADIUS = MathHelper.roundUp(BLOCK_RADIUS, 16) >> 4;

    public static final int SECTION_LENGTH = 1 + (CHUNK_RADIUS * 2);
    public static final int BLOCK_LENGTH = 16 + (BLOCK_RADIUS * 2);

    public static final int BLOCK_COUNT = BLOCK_LENGTH * BLOCK_LENGTH * BLOCK_LENGTH;
    public static final int SECTION_COUNT = SECTION_LENGTH * SECTION_LENGTH * SECTION_LENGTH;
    public static final int CHUNK_COUNT = SECTION_LENGTH * SECTION_LENGTH;

    private static final ChunkSection EMPTY_SECTION = new ChunkSection(0);

    private final BlockState[] blockStates;
    private final ChunkNibbleArray[] blockLightArrays;
    private final ChunkNibbleArray[] skyLightArrays;
    private final BiomeCache[] biomeCaches;
    private final BiomeArray[] biomeArrays;

    private final Map<ColorResolver, ColorizerCache> colorResolvers = new Reference2ReferenceArrayMap<>();

    private World world;
    private WorldChunk[] chunks;

    private BiomeCacheManager biomeCacheManager;

    private int chunkOffsetX;
    private int chunkOffsetY;
    private int chunkOffsetZ;

    private int blockOffsetX;
    private int blockOffsetY;
    private int blockOffsetZ;

    public static WorldChunk[] getChunks(World world, ChunkSectionPos pos) {
        WorldChunk chunk = world.getChunk(pos.getX(), pos.getZ());
        ChunkSection section = chunk.getSectionArray()[pos.getY()];

        if (section == null || section.isEmpty()) {
            return null;
        }

        int minChunkX = pos.getX() - CHUNK_RADIUS;
        int minChunkZ = pos.getZ() - CHUNK_RADIUS;

        int maxChunkX = pos.getX() + CHUNK_RADIUS;
        int maxChunkZ = pos.getZ() + CHUNK_RADIUS;

        WorldChunk[] chunks = new WorldChunk[SECTION_LENGTH * SECTION_LENGTH];

        for (int x = minChunkX; x <= maxChunkX; x++) {
            for (int z = minChunkZ; z <= maxChunkZ; z++) {
                chunks[getChunkIndex(x - minChunkX, z - minChunkZ)] = world.getChunk(x, z);
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
        final int minX = chunkPos.getMinX() - BLOCK_RADIUS;
        final int minY = chunkPos.getMinY() - BLOCK_RADIUS;
        final int minZ = chunkPos.getMinZ() - BLOCK_RADIUS;

        final int maxX = chunkPos.getMaxX() + BLOCK_RADIUS + 1;
        final int maxY = chunkPos.getMaxY() + BLOCK_RADIUS + 1;
        final int maxZ = chunkPos.getMaxZ() + BLOCK_RADIUS + 1;

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
        this.chunkOffsetX = chunkPos.getX() - CHUNK_RADIUS;
        this.chunkOffsetY = chunkPos.getY() - CHUNK_RADIUS;
        this.chunkOffsetZ = chunkPos.getZ() - CHUNK_RADIUS;

        ChunkLightingView blockLightProvider = this.world.getLightingProvider().get(LightType.BLOCK);
        ChunkLightingView skyLightProvider = this.world.getLightingProvider().get(LightType.SKY);

        for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
            for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                int chunkXLocal = chunkX - this.chunkOffsetX;
                int chunkZLocal = chunkZ - this.chunkOffsetZ;
                int chunkIdx = getChunkIndex(chunkXLocal, chunkZLocal);

                WorldChunk chunk = chunks[chunkIdx];

                this.biomeArrays[chunkIdx] = chunk.getBiomeArray();

                int aX = Math.max(minX, chunkX << 4);
                int bX = Math.min(maxX, (chunkX + 1) << 4);

                int aZ = Math.max(minZ, chunkZ << 4);
                int bZ = Math.min(maxZ, (chunkZ + 1) << 4);

                for (int chunkY = minChunkY; chunkY <= maxChunkY; chunkY++) {
                    int chunkYLocal = chunkY - this.chunkOffsetY;

                    ChunkSectionPos sectionPos = ChunkSectionPos.from(chunkX, chunkY, chunkZ);
                    int sectionIdx = getSectionIndex(chunkXLocal, chunkYLocal, chunkZLocal);

                    this.blockLightArrays[sectionIdx] = blockLightProvider.getLightArray(sectionPos);
                    this.skyLightArrays[sectionIdx] = skyLightProvider.getLightArray(sectionPos);

                    ChunkSection section = EMPTY_SECTION;

                    if (chunkY >= 0 && chunkY < 16) {
                        section = chunk.getSectionArray()[chunkY];

                        if (section == null) {
                            section = EMPTY_SECTION;
                        }
                    }

                    int aY = Math.max(minY, chunkY << 4);
                    int bY = Math.min(maxY, (chunkY + 1) << 4);

                    for (int y = aY; y < bY; y++) {
                        for (int z = aZ; z < bZ; z++) {
                            for (int x = aX; x < bX; x++) {
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

    private ColorizerCache getColorizerCache(ColorResolver resolver) {
        ColorizerCache cache = this.colorResolvers.get(resolver);

        if (cache == null) {
            this.colorResolvers.put(resolver, cache = new ColorizerCache(resolver, this));
        }

        return cache;
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

    // FIX: Do not access state on the main thread
    @Override
    public int getColor(BlockPos pos, ColorResolver resolver) {
        return this.getColorizerCache(resolver).getColor(pos);
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

    // FIX: Do not access state on the main thread
    @Override
    public Biome getBiomeForNoiseGen(int x, int y, int z) {
        BiomeArray array = this.biomeArrays[this.getBiomeIndexForBlock(x, z)];

        if (array != null ) {
            return array.getBiomeForNoiseGen(x, y, z);
        }

        return this.world.getGeneratorStoredBiome(x, y, z);
    }

    public Biome getCachedBiome(int x, int y, int z) {
        return this.biomeCaches[this.getChunkIndexForBlock(x, z)].getBiome(this, x, y, z);
    }

    private int getBlockIndex(int x, int y, int z) {
        int x2 = x - this.blockOffsetX;
        int y2 = y - this.blockOffsetY;
        int z2 = z - this.blockOffsetZ;

        return (y2 * BLOCK_LENGTH * BLOCK_LENGTH) + (z2 * BLOCK_LENGTH) + x2;
    }

    private int getChunkIndexForBlock(BlockPos pos) {
        return this.getChunkIndexForBlock(pos.getX(), pos.getZ());
    }

    private int getChunkIndexForBlock(int x, int z) {
        int x2 = (x >> 4) - this.chunkOffsetX;
        int z2 = (z >> 4) - this.chunkOffsetZ;

        return getChunkIndex(x2, z2);
    }

    private int getBiomeIndexForBlock(int x, int z) {
        // Coordinates are in biome space!
        // [VanillaCopy] WorldView#getBiomeForNoiseGen(int, int, int)
        int x2 = (x >> 2) - this.chunkOffsetX;
        int z2 = (z >> 2) - this.chunkOffsetZ;

        return getChunkIndex(x2, z2);
    }

    private int getSectionIndexForBlock(int x, int y, int z) {
        int x2 = (x >> 4) - this.chunkOffsetX;
        int y2 = (y >> 4) - this.chunkOffsetY;
        int z2 = (z >> 4) - this.chunkOffsetZ;

        return getSectionIndex(x2, y2, z2);
    }

    public static int getChunkIndex(int x, int z) {
        return (z * SECTION_LENGTH) + x;
    }

    public static int getSectionIndex(int x, int y, int z) {
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
