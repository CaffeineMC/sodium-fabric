package me.jellysquid.mods.sodium.client.render.chunk;

import me.jellysquid.mods.sodium.client.render.LightDataCache;
import me.jellysquid.mods.sodium.client.world.ClientWorldExtended;
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
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkNibbleArray;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.WorldChunk;
import net.minecraft.world.chunk.light.LightingProvider;
import net.minecraft.world.level.ColorResolver;

public class ChunkSlice implements BlockRenderView, BiomeAccess.Storage {
    private static final int BLOCK_MARGIN = 1;
    private static final int CHUNK_MARGIN = MathHelper.roundUp(1, 16) >> 4;

    private static final int CHUNK_LENGTH = 1 + (CHUNK_MARGIN * 2);
    private static final int BLOCK_LENGTH = 16 + (BLOCK_MARGIN * 2);

    private static final ChunkSection EMPTY_SECTION = new ChunkSection(0);

    private final int chunkXOffset;
    private final int chunkYOffset;
    private final int chunkZOffset;

    private final int offsetX;
    private final int offsetY;
    private final int offsetZ;

    private final WorldChunk[][] chunks;
    private final BlockState[] blockStates;
    private final World world;
    private final LightDataCache lightCache;

    private final ChunkNibbleArray[] blockLightArrays;
    private final ChunkNibbleArray[] skyLightArrays;
    
    private final BiomeAccess biomeAccess;

    public static ChunkSlice tryCreate(World world, ChunkSectionPos pos) {
        WorldChunk chunk = world.getChunk(pos.getX(), pos.getZ());
        ChunkSection section = chunk.getSectionArray()[pos.getY()];

        if (section == null || section.isEmpty()) {
            return null;
        }

        int minChunkX = pos.getX() - CHUNK_MARGIN;
        int minChunkZ = pos.getZ() - CHUNK_MARGIN;

        int maxChunkX = pos.getX() + CHUNK_MARGIN;
        int maxChunkZ = pos.getZ() + CHUNK_MARGIN;

        WorldChunk[][] chunks = new WorldChunk[CHUNK_LENGTH][CHUNK_LENGTH];

        for (int x = minChunkX; x <= maxChunkX; x++) {
            for (int z = minChunkZ; z <= maxChunkZ; z++) {
                chunks[x - minChunkX][z - minChunkZ] = world.getChunk(x, z);
            }
        }

        return new ChunkSlice(world, pos, chunks);
    }

    public ChunkSlice(World world, ChunkSectionPos chunkPos, WorldChunk[][] chunks) {
        this.world = world;
        this.chunks = chunks;

        this.chunkXOffset = chunkPos.getX() - CHUNK_MARGIN;
        this.chunkYOffset = chunkPos.getY() - CHUNK_MARGIN;
        this.chunkZOffset = chunkPos.getZ() - CHUNK_MARGIN;

        this.blockStates = new BlockState[BLOCK_LENGTH * BLOCK_LENGTH * BLOCK_LENGTH];

        final int minX = chunkPos.getMinX() - BLOCK_MARGIN;
        final int minY = chunkPos.getMinY() - BLOCK_MARGIN;
        final int minZ = chunkPos.getMinZ() - BLOCK_MARGIN;

        final int maxX = chunkPos.getMaxX() + BLOCK_MARGIN + 1;
        final int maxY = chunkPos.getMaxY() + BLOCK_MARGIN + 1;
        final int maxZ = chunkPos.getMaxZ() + BLOCK_MARGIN + 1;

        this.offsetX = minX;
        this.offsetY = minY;
        this.offsetZ = minZ;

        final int minChunkX = minX >> 4;
        final int maxChunkX = maxX >> 4;

        final int minChunkY = minY >> 4;
        final int maxChunkY = maxY >> 4;

        final int minChunkZ = minZ >> 4;
        final int maxChunkZ = maxZ >> 4;

        this.blockLightArrays = new ChunkNibbleArray[3 * 3 * 3];
        this.skyLightArrays = new ChunkNibbleArray[3 * 3 * 3];

        for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
            for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                int chunkXLocal = chunkX - this.chunkXOffset;
                int chunkZLocal = chunkZ - this.chunkZOffset;

                WorldChunk chunk = chunks[chunkXLocal][chunkZLocal];

                int aX = Math.max(minX, chunkX << 4);
                int bX = Math.min(maxX, (chunkX + 1) << 4);

                int aZ = Math.max(minZ, chunkZ << 4);
                int bZ = Math.min(maxZ, (chunkZ + 1) << 4);

                for (int chunkY = minChunkY; chunkY <= maxChunkY; chunkY++) {
                    int chunkYLocal = chunkY - this.chunkYOffset;

                    ChunkSectionPos p = ChunkSectionPos.from(chunkX, chunkY, chunkZ);
                    int i = getChunkIndex(chunkXLocal, chunkYLocal, chunkZLocal);

                    this.blockLightArrays[i] = this.world.getLightingProvider().get(LightType.BLOCK).getLightArray(p);
                    this.skyLightArrays[i] = this.world.getLightingProvider().get(LightType.SKY).getLightArray(p);

                    ChunkSection section = getSection(chunk, chunkY);

                    int aY = Math.max(minY, chunkY << 4);
                    int bY = Math.min(maxY, (chunkY + 1) << 4);

                    for (int y = aY; y < bY; y++) {
                        for (int z = aZ; z < bZ; z++) {
                            for (int x = aX; x < bX; x++) {
                                this.blockStates[this.getIndex(x, y, z)] = section.getBlockState(x & 15, y & 15, z & 15);
                            }
                        }
                    }
                }
            }
        }

        this.lightCache = new LightDataCache(this, minX, minY, minZ, BLOCK_LENGTH, BLOCK_LENGTH, BLOCK_LENGTH);
        this.biomeAccess = new BiomeAccess(this, this.world.getSeed(), this.world.getDimension().getType().getBiomeAccessType());
    }

    private static ChunkSection getSection(WorldChunk chunk, int y) {
        if (chunk == null || y < 0 || y >= 16) {
            return EMPTY_SECTION;
        }

        ChunkSection section = chunk.getSectionArray()[y];

        if (section == null) {
            return EMPTY_SECTION;
        }

        return section;
    }

    private final int getIndex(BlockPos pos) {
        return this.getIndex(pos.getX(), pos.getY(), pos.getZ());
    }

    protected int getIndex(int x, int y, int z) {
        return ((y - this.offsetY) * BLOCK_LENGTH * BLOCK_LENGTH) + ((z - this.offsetZ) * BLOCK_LENGTH) + x - this.offsetX;
    }

    @Override
    public BlockState getBlockState(BlockPos pos) {
        return this.blockStates[this.getIndex(pos)];
    }

    public BlockState getBlockState(int x, int y, int z) {
        return this.blockStates[this.getIndex(x, y, z)];
    }

    @Override
    public FluidState getFluidState(BlockPos pos) {
        return this.getBlockState(pos).getFluidState();
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
        int x = (pos.getX() >> 4) - this.chunkXOffset;
        int z = (pos.getZ() >> 4) - this.chunkZOffset;

        return this.chunks[x][z].getBlockEntity(pos, type);
    }

    // FIX: Do not access state on the main thread
    @Override
    public int getColor(BlockPos pos, ColorResolver resolver) {
        return ((ClientWorldExtended) this.world).getColor(pos, resolver, this.biomeAccess);
    }

    @Override
    public int getLightLevel(LightType type, BlockPos pos) {
        ChunkNibbleArray[] arrays = type == LightType.SKY ? this.skyLightArrays : this.blockLightArrays;

        int x = pos.getX();
        int y = pos.getY();
        int z = pos.getZ();

        int chunkX = (x >> 4) - this.chunkXOffset;
        int chunkY = (y >> 4) - this.chunkYOffset;
        int chunkZ = (z >> 4) - this.chunkZOffset;

        ChunkNibbleArray array = arrays[getChunkIndex(chunkX, chunkY, chunkZ)];

        if (array != null) {
            return array.get(x & 15, y & 15, z & 15);
        }

        return 0;
    }

    private int getChunkIndex(int x, int y, int z) {
        return (y * CHUNK_LENGTH * CHUNK_LENGTH) + (z * CHUNK_LENGTH) + x;
    }

    public LightDataCache getLightDataCache() {
        return this.lightCache;
    }

    // FIX: Do not access state on the main thread
    @Override
    public Biome getBiomeForNoiseGen(int x, int y, int z) {
        // Coordinates are in biome space!
        // [VanillaCopy] WorldView#getBiomeForNoiseGen(int, int, int)
        int x2 = (x >> 2) - this.chunkXOffset;
        int z2 = (z >> 2) - this.chunkZOffset;

        Chunk chunk = this.chunks[x2][z2];

        if (chunk != null && chunk.getBiomeArray() != null) {
            return chunk.getBiomeArray().getBiomeForNoiseGen(x, y, z);
        }

        return this.world.getGeneratorStoredBiome(x, y, z);
    }
}
