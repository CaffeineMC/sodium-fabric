package me.jellysquid.mods.sodium.client.world;

import it.unimi.dsi.fastutil.ints.Int2ReferenceMap;
import me.jellysquid.mods.sodium.client.world.biome.BiomeColorCache;
import me.jellysquid.mods.sodium.client.world.biome.BiomeColorSource;
import me.jellysquid.mods.sodium.client.world.biome.BiomeColorView;
import me.jellysquid.mods.sodium.client.world.biome.BiomeSlice;
import me.jellysquid.mods.sodium.client.world.cloned.ChunkRenderContext;
import me.jellysquid.mods.sodium.client.world.cloned.ClonedChunkSection;
import me.jellysquid.mods.sodium.client.world.cloned.ClonedChunkSectionCache;
import net.fabricmc.fabric.api.rendering.data.v1.RenderAttachedBlockView;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.fluid.FluidState;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.math.*;
import net.minecraft.world.BlockRenderView;
import net.minecraft.world.LightType;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.ColorResolver;
import net.minecraft.world.chunk.ChunkNibbleArray;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.WorldChunk;
import net.minecraft.world.chunk.light.LightingProvider;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Objects;

/**
 * <p>Takes a slice of world state (block states, biome and light data arrays) and copies the data for use in off-thread
 * operations. This allows chunk build tasks to see a consistent snapshot of chunk data at the exact moment the task was
 * created.</p>
 *
 * <p>World slices are not safe to use from multiple threads at once, but the data they contain is safe from modification
 * by the main client thread.</p>
 *
 * <p>Object pooling should be used to avoid huge allocations as this class contains many large arrays.</p>
 */
public final class WorldSlice implements BlockRenderView, BiomeColorView, RenderAttachedBlockView {
    private static final LightType[] LIGHT_TYPES = LightType.values();

    // The number of blocks in a section.
    private static final int SECTION_BLOCK_COUNT = 16 * 16 * 16;

    // The radius of blocks around the origin chunk that should be copied.
    private static final int NEIGHBOR_BLOCK_RADIUS = 2;

    // The radius of chunks around the origin chunk that should be copied.
    private static final int NEIGHBOR_CHUNK_RADIUS = MathHelper.roundUpToMultiple(NEIGHBOR_BLOCK_RADIUS, 16) >> 4;

    // The number of sections on each axis of this slice.
    private static final int SECTION_ARRAY_LENGTH = 1 + (NEIGHBOR_CHUNK_RADIUS * 2);

    // The size of the (Local Section -> Resource) arrays.
    private static final int SECTION_ARRAY_SIZE = SECTION_ARRAY_LENGTH * SECTION_ARRAY_LENGTH * SECTION_ARRAY_LENGTH;

    // The number of bits needed for each local X/Y/Z coordinate.
    private static final int LOCAL_XYZ_BITS = 4;

    // The default block state used for out-of-bounds access
    private static final BlockState EMPTY_BLOCK_STATE = Blocks.AIR.getDefaultState();

    // The world this slice has copied data from
    private final ClientWorld world;

    // The accessor used for fetching biome data from the slice
    private final BiomeSlice biomeSlice;

    // The biome blend cache
    private final BiomeColorCache biomeColors;

    // (Local Section -> Block States) table.
    private final BlockState[][] blockArrays;

    // (Local Section -> Light Arrays) table.
    private final @Nullable ChunkNibbleArray[][] lightArrays;

    // (Local Section -> Block Entity) table.
    private final @Nullable Int2ReferenceMap<BlockEntity>[] blockEntityArrays;

    // (Local Section -> Block Entity Render Data) table.
    private final @Nullable Int2ReferenceMap<Object>[] blockEntityRenderDataArrays;

    // The starting point from which this slice captures blocks
    private int originX, originY, originZ;
    
    // The volume that this WorldSlice contains
    private BlockBox volume;

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

        ClonedChunkSection[] sections = new ClonedChunkSection[SECTION_ARRAY_SIZE];

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

    @SuppressWarnings("unchecked")
    public WorldSlice(ClientWorld world) {
        this.world = world;

        this.blockArrays = new BlockState[SECTION_ARRAY_SIZE][SECTION_BLOCK_COUNT];
        this.lightArrays = new ChunkNibbleArray[SECTION_ARRAY_SIZE][LIGHT_TYPES.length];

        this.blockEntityArrays = new Int2ReferenceMap[SECTION_ARRAY_SIZE];
        this.blockEntityRenderDataArrays = new Int2ReferenceMap[SECTION_ARRAY_SIZE];

        this.biomeSlice = new BiomeSlice();
        this.biomeColors = new BiomeColorCache(this.biomeSlice, MinecraftClient.getInstance().options.getBiomeBlendRadius().getValue());

        for (BlockState[] blockArray : this.blockArrays) {
            Arrays.fill(blockArray, EMPTY_BLOCK_STATE);
        }
    }

    public void copyData(ChunkRenderContext context) {
        this.originX = (context.getOrigin().getX() - NEIGHBOR_CHUNK_RADIUS) << 4;
        this.originY = (context.getOrigin().getY() - NEIGHBOR_CHUNK_RADIUS) << 4;
        this.originZ = (context.getOrigin().getZ() - NEIGHBOR_CHUNK_RADIUS) << 4;
        this.volume = context.getVolume();

        for (int x = 0; x < SECTION_ARRAY_LENGTH; x++) {
            for (int y = 0; y < SECTION_ARRAY_LENGTH; y++) {
                for (int z = 0; z < SECTION_ARRAY_LENGTH; z++) {
                    this.copySectionData(context, getLocalSectionIndex(x, y, z));
                }
            }
        }

        this.biomeSlice.update(this.world, context);
        this.biomeColors.update(context);
    }

    private void copySectionData(ChunkRenderContext context, int sectionIndex) {
        var section = context.getSections()[sectionIndex];

        Objects.requireNonNull(section, "Chunk section must be non-null");

        this.unpackBlockData(this.blockArrays[sectionIndex], context, section);

        this.lightArrays[sectionIndex][LightType.BLOCK.ordinal()] = section.getLightArray(LightType.BLOCK);
        this.lightArrays[sectionIndex][LightType.SKY.ordinal()] = section.getLightArray(LightType.SKY);

        this.blockEntityArrays[sectionIndex] = section.getBlockEntityMap();
        this.blockEntityRenderDataArrays[sectionIndex] = section.getBlockEntityRenderDataMap();
    }

    private void unpackBlockData(BlockState[] blockArray, ChunkRenderContext context, ClonedChunkSection section) {
        if (section.getBlockData() == null) {
            Arrays.fill(blockArray, EMPTY_BLOCK_STATE);
            return;
        }

        var container = ReadableContainerExtended.of(section.getBlockData());

        ChunkSectionPos origin = context.getOrigin();
        ChunkSectionPos pos = section.getPosition();

        if (origin.equals(pos))  {
            container.sodium$unpack(blockArray);
        } else {
            var bounds = context.getVolume();

            int minBlockX = Math.max(bounds.getMinX(), pos.getMinX());
            int maxBlockX = Math.min(bounds.getMaxX(), pos.getMaxX());

            int minBlockY = Math.max(bounds.getMinY(), pos.getMinY());
            int maxBlockY = Math.min(bounds.getMaxY(), pos.getMaxY());

            int minBlockZ = Math.max(bounds.getMinZ(), pos.getMinZ());
            int maxBlockZ = Math.min(bounds.getMaxZ(), pos.getMaxZ());

            container.sodium$unpack(blockArray, minBlockX & 15, minBlockY & 15, minBlockZ & 15,
                    maxBlockX & 15, maxBlockY & 15, maxBlockZ & 15);
        }
    }

    public void reset() {
        // erase any pointers to resources we no longer need
        // no point in cleaning the pre-allocated arrays (such as block state storage) since we hold the
        // only reference.
        for (int sectionIndex = 0; sectionIndex < SECTION_ARRAY_LENGTH; sectionIndex++) {
            Arrays.fill(this.lightArrays[sectionIndex], null);

            this.blockEntityArrays[sectionIndex] = null;
            this.blockEntityRenderDataArrays[sectionIndex] = null;
        }
    }

    @Override
    public BlockState getBlockState(BlockPos pos) {
        return this.getBlockState(pos.getX(), pos.getY(), pos.getZ());
    }

    public BlockState getBlockState(int x, int y, int z) {
        if (!this.volume.contains(x, y, z)) {
            return EMPTY_BLOCK_STATE;
        }

        int relX = x - this.originX;
        int relY = y - this.originY;
        int relZ = z - this.originZ;

        return this.blockArrays[getLocalSectionIndex(relX >> 4, relY >> 4, relZ >> 4)]
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
        // Not thread-safe to access lighting data from off-thread, even if Minecraft allows it.
        throw new UnsupportedOperationException();
    }

    @Override
    public int getLightLevel(LightType type, BlockPos pos) {
        if (!this.volume.contains(pos.getX(), pos.getY(), pos.getZ())) {
            return 0;
        }

        int relX = pos.getX() - this.originX;
        int relY = pos.getY() - this.originY;
        int relZ = pos.getZ() - this.originZ;

        var lightArray = this.lightArrays[getLocalSectionIndex(relX >> 4, relY >> 4, relZ >> 4)][type.ordinal()];

        if (lightArray == null) {
            // If the array is null, it means the dimension for the current world does not support that light type
            return 0;
        }

        return lightArray.get(relX & 15, relY & 15, relZ & 15);
    }

    @Override
    public int getBaseLightLevel(BlockPos pos, int ambientDarkness) {
        if (!this.volume.contains(pos.getX(), pos.getY(), pos.getZ())) {
            return 0;
        }

        int relX = pos.getX() - this.originX;
        int relY = pos.getY() - this.originY;
        int relZ = pos.getZ() - this.originZ;

        var lightArrays = this.lightArrays[getLocalSectionIndex(relX >> 4, relY >> 4, relZ >> 4)];

        var skyLightArray = lightArrays[LightType.SKY.ordinal()];
        var blockLightArray = lightArrays[LightType.BLOCK.ordinal()];

        int localX = relX & 15;
        int localY = relY & 15;
        int localZ = relZ & 15;

        int skyLight = skyLightArray == null ? 0 : skyLightArray.get(localX, localY, localZ) - ambientDarkness;
        int blockLight = blockLightArray == null ? 0 : blockLightArray.get(localX, localY, localZ);

        return Math.max(blockLight, skyLight);
    }

    @Override
    public BlockEntity getBlockEntity(BlockPos pos) {
        return this.getBlockEntity(pos.getX(), pos.getY(), pos.getZ());
    }

    public BlockEntity getBlockEntity(int x, int y, int z) {
        if (!this.volume.contains(x, y, z)) {
            return null;
        }

        int relX = x - this.originX;
        int relY = y - this.originY;
        int relZ = z - this.originZ;

        var blockEntities = this.blockEntityArrays[getLocalSectionIndex(relX >> 4, relY >> 4, relZ >> 4)];

        if (blockEntities == null) {
            return null;
        }

        return blockEntities.get(getLocalBlockIndex(relX & 15, relY & 15, relZ & 15));
    }

    @Override
    public int getColor(BlockPos pos, ColorResolver resolver) {
        return this.biomeColors.getColor(BiomeColorSource.from(resolver), pos.getX(), pos.getY(), pos.getZ());
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
    public int getColor(BiomeColorSource source, int x, int y, int z) {
        return this.biomeColors.getColor(source, x, y, z);
    }

    @Override
    public @Nullable Object getBlockEntityRenderData(BlockPos pos) {
        if (!this.volume.contains(pos.getX(), pos.getY(), pos.getZ())) {
            return null;
        }

        int relX = pos.getX() - this.originX;
        int relY = pos.getY() - this.originY;
        int relZ = pos.getZ() - this.originZ;

        var blockEntityRenderDataMap = this.blockEntityRenderDataArrays[getLocalSectionIndex(relX >> 4, relY >> 4, relZ >> 4)];

        if (blockEntityRenderDataMap == null) {
            return null;
        }

        return blockEntityRenderDataMap.get(getLocalBlockIndex(relX & 15, relY & 15, relZ & 15));
    }

    @Override
    public boolean hasBiomes() {
        return true;
    }

    @Override
    public RegistryEntry<Biome> getBiomeFabric(BlockPos pos) {
        return this.biomeSlice.getBiome(pos.getX(), pos.getY(), pos.getZ());
    }

    public static int getLocalBlockIndex(int x, int y, int z) {
        return (y << LOCAL_XYZ_BITS << LOCAL_XYZ_BITS) | (z << LOCAL_XYZ_BITS) | x;
    }

    public static int getLocalSectionIndex(int x, int y, int z) {
        return (y * SECTION_ARRAY_LENGTH * SECTION_ARRAY_LENGTH) + (z * SECTION_ARRAY_LENGTH) + x;
    }
}
