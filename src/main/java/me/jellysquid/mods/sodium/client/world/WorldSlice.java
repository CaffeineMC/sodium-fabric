package me.jellysquid.mods.sodium.client.world;

import me.jellysquid.mods.sodium.client.world.biome.BlockColorCache;
import me.jellysquid.mods.sodium.client.world.cloned.ChunkRenderContext;
import me.jellysquid.mods.sodium.client.world.cloned.ClonedChunkSection;
import me.jellysquid.mods.sodium.client.world.cloned.ClonedChunkSectionCache;
import me.jellysquid.mods.sodium.client.world.cloned.PackedIntegerArrayExtended;
import me.jellysquid.mods.sodium.client.world.cloned.palette.ClonedPalette;
import net.fabricmc.fabric.api.rendering.data.v1.RenderAttachedBlockView;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.fluid.FluidState;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.collection.PackedIntegerArray;
import net.minecraft.util.math.*;
import net.minecraft.world.BlockRenderView;
import net.minecraft.world.LightType;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.ColorResolver;
import net.minecraft.world.biome.source.BiomeAccess;
import net.minecraft.world.biome.source.BiomeCoords;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.WorldChunk;
import net.minecraft.world.chunk.light.LightingProvider;
import org.jetbrains.annotations.Nullable;

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
public class WorldSlice implements BlockRenderView, RenderAttachedBlockView {
    // The number of blocks in a section.
    private static final int SECTION_BLOCK_COUNT = 16 * 16 * 16;

    // The number of biomes in a section.
    private static final int SECTION_BIOME_COUNT = 4 * 4 * 4;

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

    // The number of bits needed for each X/Y/Z block coordinate.
    private static final int BLOCK_BITS = 4;

    // The number of bits needed for each X/Y/Z biome coordinate.
    private static final int BIOME_BITS = 2;

    // The array size for the section lookup table.
    private static final int SECTION_TABLE_ARRAY_SIZE = TABLE_LENGTH * TABLE_LENGTH * TABLE_LENGTH;

    // The world this slice has copied data from
    private final World world;

    // The accessor used for fetching biome data from the slice
    private final BiomeAccess biomeAccess;

    // Local Section->BlockState table.
    private final BlockState[][] blockStatesArrays;

    // Local Section->Biome table.
    private final RegistryEntry<Biome>[][] biomeArrays;

    // Local section copies. Read-only.
    private ClonedChunkSection[] sections;

    // The biome blend cache
    private BlockColorCache biomeColors;

    // The starting point from which this slice captures blocks
    private int baseX, baseY, baseZ;

    // The chunk origin of this slice
    private ChunkSectionPos origin;

    public static ChunkRenderContext prepare(World world, ChunkSectionPos origin, ClonedChunkSectionCache sectionCache) {
        WorldChunk chunk = world.getChunk(origin.getX(), origin.getZ());
        ChunkSection section = chunk.getSectionArray()[world.sectionCoordToIndex(origin.getY())];

        // If the chunk section is absent or empty, simply terminate now. There will never be anything in this chunk
        // section to render, so we need to signal that a chunk render task shouldn't created. This saves a considerable
        // amount of time in queueing instant build tasks and greatly accelerates how quickly the world can be loaded.
        if (section == null || section.isEmpty()) {
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

        this.biomeAccess = new BiomeAccess(this::getStoredBiome, ((BiomeSeedProvider) this.world).getBiomeSeed());

        this.sections = new ClonedChunkSection[SECTION_TABLE_ARRAY_SIZE];
        this.blockStatesArrays = new BlockState[SECTION_TABLE_ARRAY_SIZE][SECTION_BLOCK_COUNT];
        this.biomeArrays = new RegistryEntry[SECTION_TABLE_ARRAY_SIZE][SECTION_BIOME_COUNT];

    }

    public void copyData(ChunkRenderContext context) {
        this.origin = context.getOrigin();
        this.sections = context.getSections();

        this.baseX = (this.origin.getX() - NEIGHBOR_CHUNK_RADIUS) << 4;
        this.baseY = (this.origin.getY() - NEIGHBOR_CHUNK_RADIUS) << 4;
        this.baseZ = (this.origin.getZ() - NEIGHBOR_CHUNK_RADIUS) << 4;

        for (int x = 0; x < SECTION_LENGTH; x++) {
            for (int y = 0; y < SECTION_LENGTH; y++) {
                for (int z = 0; z < SECTION_LENGTH; z++) {
                    int idx = getLocalSectionIndex(x, y, z);
                    this.unpackBlockData(this.blockStatesArrays[idx], this.sections[idx], context.getVolume());
                    this.unpackBiomeData(this.biomeArrays[idx], this.sections[idx]);
                }
            }
        }

        this.biomeColors = new BlockColorCache(this, MinecraftClient.getInstance().options.getBiomeBlendRadius().getValue());
    }

    private void unpackBlockData(BlockState[] states, ClonedChunkSection section, BlockBox box) {
        if (this.origin.equals(section.getPosition()))  {
            this.unpackBlockData(states, section);
        } else {
            this.unpackBlockDataSlow(states, section, box);
        }
    }

    private void unpackBlockDataSlow(BlockState[] states, ClonedChunkSection section, BlockBox box) {
        PackedIntegerArray intArray = section.getBlockData();
        ClonedPalette<BlockState> palette = section.getBlockPalette();

        ChunkSectionPos pos = section.getPosition();

        int minBlockX = Math.max(box.getMinX(), pos.getMinX());
        int maxBlockX = Math.min(box.getMaxX(), pos.getMaxX());

        int minBlockY = Math.max(box.getMinY(), pos.getMinY());
        int maxBlockY = Math.min(box.getMaxY(), pos.getMaxY());

        int minBlockZ = Math.max(box.getMinZ(), pos.getMinZ());
        int maxBlockZ = Math.min(box.getMaxZ(), pos.getMaxZ());

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

    private void unpackBlockData(BlockState[] states, ClonedChunkSection section) {
        ((PackedIntegerArrayExtended) section.getBlockData())
                .copyUsingPalette(states, section.getBlockPalette());
    }

    private void unpackBiomeData(RegistryEntry<Biome>[] biomes, ClonedChunkSection section) {
        for (int x = 0; x < 4; x++) {
            for (int y = 0; y < 4; y++) {
                for (int z = 0; z < 4; z++) {
                    biomes[getLocalBiomeIndex(x, y, z)] = section.getBiome(x, y, z);
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
        return this.biomeColors.getColor(resolver, pos.getX(), pos.getY(), pos.getZ());
    }

    @Override
    public int getLightLevel(LightType type, BlockPos pos) {
        int relX = pos.getX() - this.baseX;
        int relY = pos.getY() - this.baseY;
        int relZ = pos.getZ() - this.baseZ;

        return this.sections[getLocalSectionIndex(relX >> 4, relY >> 4, relZ >> 4)]
                .getLightLevel(type, relX & 15, relY & 15, relZ & 15);
    }

    public ChunkSectionPos getOrigin() {
        return this.origin;
    }

    @Override
    public int getHeight() {
        return this.world.getHeight();
    }

    @Override
    public int getBottomY() {
        return this.world.getBottomY();
    }

    @Override
    public @Nullable Object getBlockEntityRenderAttachment(BlockPos pos) {
        int relX = pos.getX() - this.baseX;
        int relY = pos.getY() - this.baseY;
        int relZ = pos.getZ() - this.baseZ;

        return this.sections[WorldSlice.getLocalSectionIndex(relX >> 4, relY >> 4, relZ >> 4)]
                .getBlockEntityRenderAttachment(relX & 15, relY & 15, relZ & 15);
    }

    // Coordinates are in biome space!
    private RegistryEntry<Biome> getStoredBiome(int biomeX, int biomeY, int biomeZ) {
        int chunkX = (BiomeCoords.toBlock(biomeX) - this.baseX) >> 4;
        int chunkY = (BiomeCoords.toBlock(biomeY) - this.baseY) >> 4;
        int chunkZ = (BiomeCoords.toBlock(biomeZ) - this.baseZ) >> 4;

        return this.biomeArrays[getLocalSectionIndex(chunkX, chunkY, chunkZ)]
                [getLocalBiomeIndex(biomeX & 3, biomeY & 3, biomeZ & 3)];
    }

    public BiomeAccess getBiomeAccess() {
        return this.biomeAccess;
    }

    private static int getLocalBiomeIndex(int x, int y, int z) {
        return y << BIOME_BITS << BIOME_BITS | z << BIOME_BITS | x;
    }

    public static int getLocalBlockIndex(int x, int y, int z) {
        return y << BLOCK_BITS << BLOCK_BITS | z << BLOCK_BITS | x;
    }

    public static int getLocalSectionIndex(int x, int y, int z) {
        return y << TABLE_BITS << TABLE_BITS | z << TABLE_BITS | x;
    }
}
