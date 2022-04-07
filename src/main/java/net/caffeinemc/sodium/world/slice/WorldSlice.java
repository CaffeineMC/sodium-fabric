package net.caffeinemc.sodium.world.slice;

import net.caffeinemc.sodium.interop.vanilla.mixin.BiomeSeedProvider;
import net.caffeinemc.sodium.world.biome.ChunkColorCache;
import net.caffeinemc.sodium.world.slice.cloned.ClonedChunkSection;
import net.caffeinemc.sodium.interop.vanilla.mixin.PackedIntegerArrayExtended;
import net.fabricmc.fabric.api.rendering.data.v1.RenderAttachedBlockView;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.fluid.FluidState;
import net.minecraft.util.math.*;
import net.minecraft.util.registry.RegistryEntry;
import net.minecraft.world.BlockRenderView;
import net.minecraft.world.LightType;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.source.BiomeAccess;
import net.minecraft.world.biome.source.BiomeCoords;
import net.minecraft.world.chunk.light.LightingProvider;
import net.minecraft.world.level.ColorResolver;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;

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

    // The radius of blocks around the origin chunk section that should be copied.
    static final int NEIGHBOR_BLOCK_RADIUS = 2;

    // The radius of sections around the origin chunk section that should be copied.
    static final int NEIGHBOR_SECTION_RADIUS = ChunkSectionPos.getSectionCoord(MathHelper.roundUpToMultiple(NEIGHBOR_BLOCK_RADIUS, 16));

    // The number of sections on each axis of this slice.
    private static final int SECTION_LENGTH = 1 + (NEIGHBOR_SECTION_RADIUS * 2);

    // The size of the lookup tables used for mapping values to coordinate int pairs. The lookup table size is always
    // a power of two so that multiplications can be replaced with simple bit shifts in hot code paths.
    private static final int TABLE_LENGTH = MathHelper.smallestEncompassingPowerOfTwo(SECTION_LENGTH);

    // The number of bits needed for each X/Y/Z component in a lookup table.
    private static final int TABLE_BITS = Integer.bitCount(TABLE_LENGTH - 1);

    // The array size for the section lookup table.
    static final int SECTION_TABLE_ARRAY_SIZE = TABLE_LENGTH * TABLE_LENGTH * TABLE_LENGTH;

    // The number of bits needed for each X/Y/Z block coordinate in packed format.
    private static final int PACKED_BLOCK_BITS = 4;

    // The number of bits needed for each X/Y/Z biome coordinate in packed format.
    private static final int PACKED_BIOME_BITS = 2;

    // The world this slice has copied data from
    private final World world;

    // The accessor used for fetching biome data from the slice
    private final BiomeAccess biomeAccess;

    // Local Section->BlockState table.
    private final BlockState[][] blockStatesArrays;

    // Local Section->Biome table.
    private final RegistryEntry<Biome>[][] biomeArrays;

    // Local section copies. Read-only.
    private ClonedChunkSection[] clonedSections;

    // The biome blend cache
    private ChunkColorCache biomeColorCache;

    // The starting point from which this slice captures chunk sections
    private int offsetSectionX, offsetSectionY, offsetSectionZ;

    // The origin of this slice in section-coordinate space
    private ChunkSectionPos origin;

    public WorldSlice(World world) {
        this.world = world;

        this.biomeAccess = new BiomeAccess(this::getStoredBiome, ((BiomeSeedProvider) this.world).getBiomeSeed());

        this.clonedSections = new ClonedChunkSection[SECTION_TABLE_ARRAY_SIZE];
        this.blockStatesArrays = new BlockState[SECTION_TABLE_ARRAY_SIZE][SECTION_BLOCK_COUNT];

        //noinspection unchecked
        this.biomeArrays = new RegistryEntry[SECTION_TABLE_ARRAY_SIZE][SECTION_BIOME_COUNT];
    }

    public void init(WorldSliceData context) {
        this.origin = context.getOrigin();
        this.clonedSections = context.getSections();

        this.offsetSectionX = this.origin.getX() - NEIGHBOR_SECTION_RADIUS;
        this.offsetSectionY = this.origin.getY() - NEIGHBOR_SECTION_RADIUS;
        this.offsetSectionZ = this.origin.getZ() - NEIGHBOR_SECTION_RADIUS;

        for (int x = 0; x < SECTION_LENGTH; x++) {
            for (int y = 0; y < SECTION_LENGTH; y++) {
                for (int z = 0; z < SECTION_LENGTH; z++) {
                    int idx = packSectionIndex(x, y, z);
                    this.unpackBlockData(this.blockStatesArrays[idx], this.clonedSections[idx], context.getVolume());
                    this.unpackBiomeData(this.biomeArrays[idx], this.clonedSections[idx]);
                }
            }
        }

        var options = MinecraftClient.getInstance().options;
        var colorBlendDistance = MathHelper.clamp(options.biomeBlendRadius, 0, 7);

        this.biomeColorCache = new ChunkColorCache(this.origin, this.biomeAccess, colorBlendDistance);
    }

    private void unpackBlockData(BlockState[] states, ClonedChunkSection section, BlockBox box) {
        if (section.isEmpty()) {
            Arrays.fill(states, Blocks.AIR.getDefaultState());
        } else if (this.origin.equals(section.getPosition()))  {
            this.unpackBlockData(states, section);
        } else {
            this.unpackBlockDataSlow(states, section, box);
        }
    }

    private void unpackBlockDataSlow(BlockState[] states, ClonedChunkSection section, BlockBox box) {
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
                    int blockIdx = getBlockIndex(x, y, z);
                    states[blockIdx] = section.getBlockState(blockIdx);
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
                    biomes[packLocalBiomeIndex(x, y, z)] = section.getBiome(x, y, z);
                }
            }
        }
    }

    @Override
    public BlockState getBlockState(BlockPos pos) {
        return this.getBlockState(pos.getX(), pos.getY(), pos.getZ());
    }

    public BlockState getBlockState(int blockX, int blockY, int blockZ) {
        var blockData = this.blockStatesArrays[this.getSectionIndexFromBlockCoord(blockX, blockY, blockZ)];

        return blockData[getBlockIndex(blockX, blockY, blockZ)];
    }

    @Override
    public FluidState getFluidState(BlockPos pos) {
        var blockState = this.getBlockState(pos);
        return blockState.getFluidState();
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

    public BlockEntity getBlockEntity(int blockX, int blockY, int blockZ) {
        var section = this.clonedSections[this.getSectionIndexFromBlockCoord(blockX, blockY, blockZ)];

        return section.getBlockEntity(
                ChunkSectionPos.getLocalCoord(blockX),
                ChunkSectionPos.getLocalCoord(blockY),
                ChunkSectionPos.getLocalCoord(blockZ));
    }

    @Override
    public int getColor(BlockPos pos, ColorResolver resolver) {
        return this.biomeColorCache.getColor(resolver, pos.getX(), pos.getY(), pos.getZ());
    }

    @Override
    public int getLightLevel(LightType type, BlockPos pos) {
        return this.getLightLevel(type, pos.getX(), pos.getY(), pos.getZ());
    }

    public int getLightLevel(LightType type, int blockX, int blockY, int blockZ) {
        var section = this.clonedSections[this.getSectionIndexFromBlockCoord(blockX, blockY, blockZ)];

        return section.getLightLevel(type,
                ChunkSectionPos.getLocalCoord(blockX),
                ChunkSectionPos.getLocalCoord(blockY),
                ChunkSectionPos.getLocalCoord(blockZ));
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
        return this.getBlockEntityRenderAttachment(pos.getX(), pos.getY(), pos.getZ());
    }

    private Object getBlockEntityRenderAttachment(int blockX, int blockY, int blockZ) {
        var section = this.clonedSections[this.getSectionIndexFromBlockCoord(blockX, blockY, blockZ)];

        return section.getBlockEntityRenderAttachment(
                        ChunkSectionPos.getLocalCoord(blockX),
                        ChunkSectionPos.getLocalCoord(blockY),
                        ChunkSectionPos.getLocalCoord(blockZ));
    }

    // Coordinates are in biome space!
    private RegistryEntry<Biome> getStoredBiome(int biomeX, int biomeY, int biomeZ) {
        var biomeArray = this.biomeArrays[this.getSectionIndexFromBiomeCoord(biomeX, biomeY, biomeZ)];

        return biomeArray[packLocalBiomeIndex(BiomeCoords.method_39920(biomeX), BiomeCoords.method_39920(biomeY), BiomeCoords.method_39920(biomeZ))];
    }

    private static int packLocalBiomeIndex(int localBiomeX, int localBiomeY, int localBiomeZ) {
        return localBiomeY << PACKED_BIOME_BITS << PACKED_BIOME_BITS | localBiomeZ << PACKED_BIOME_BITS | localBiomeX;
    }

    private static int packLocalBlockIndex(int localBlockX, int localBlockY, int localBlockZ) {
        return localBlockY << PACKED_BLOCK_BITS << PACKED_BLOCK_BITS | localBlockZ << PACKED_BLOCK_BITS | localBlockX;
    }

    static int packSectionIndex(int localSectionX, int localSectionY, int localSectionZ) {
        return localSectionY << TABLE_BITS << TABLE_BITS | localSectionZ << TABLE_BITS | localSectionX;
    }

    private static int getBlockIndex(int blockX, int blockY, int blockZ) {
        return packLocalBlockIndex(
                ChunkSectionPos.getLocalCoord(blockX),
                ChunkSectionPos.getLocalCoord(blockY),
                ChunkSectionPos.getLocalCoord(blockZ));
    }

    private int getSectionIndexFromBiomeCoord(int biomeX, int biomeY, int biomeZ) {
        return this.getSectionIndexFromSectionCoord(BiomeCoords.toChunk(biomeX), BiomeCoords.toChunk(biomeY), BiomeCoords.toChunk(biomeZ));
    }

    private int getSectionIndexFromSectionCoord(int sectionX, int sectionY, int sectionZ) {
        return packSectionIndex(sectionX - this.offsetSectionX, sectionY - this.offsetSectionY, sectionZ - this.offsetSectionZ);
    }

    private int getSectionIndexFromBlockCoord(int blockX, int blockY, int blockZ) {
        return this.getSectionIndexFromSectionCoord(
                ChunkSectionPos.getSectionCoord(blockX),
                ChunkSectionPos.getSectionCoord(blockY),
                ChunkSectionPos.getSectionCoord(blockZ));
    }
}
