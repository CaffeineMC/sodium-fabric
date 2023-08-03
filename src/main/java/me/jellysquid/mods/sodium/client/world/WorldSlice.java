package me.jellysquid.mods.sodium.client.world;

import it.unimi.dsi.fastutil.ints.Int2ReferenceMap;
import me.jellysquid.mods.sodium.client.world.biome.BiomeColorCache;
import me.jellysquid.mods.sodium.client.world.biome.BiomeColorSource;
import me.jellysquid.mods.sodium.client.world.biome.BiomeColorView;
import me.jellysquid.mods.sodium.client.world.biome.BiomeSlice;
import me.jellysquid.mods.sodium.client.world.cloned.ChunkRenderContext;
import me.jellysquid.mods.sodium.client.world.cloned.ClonedChunkSection;
import me.jellysquid.mods.sodium.client.world.cloned.ClonedChunkSectionCache;
import me.jellysquid.mods.sodium.client.world.cloned.PackedIntegerArrayExtended;
import me.jellysquid.mods.sodium.client.world.cloned.palette.ClonedPalette;
import net.fabricmc.fabric.api.rendering.data.v1.RenderAttachedBlockView;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.fluid.FluidState;
import net.minecraft.util.collection.PackedIntegerArray;
import net.minecraft.util.math.*;
import net.minecraft.world.BlockRenderView;
import net.minecraft.world.LightType;
import net.minecraft.world.World;
import net.minecraft.world.biome.ColorResolver;
import net.minecraft.world.chunk.ChunkNibbleArray;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.WorldChunk;
import net.minecraft.world.chunk.light.LightingProvider;
import org.apache.commons.lang3.Validate;
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
public final class WorldSlice implements BlockRenderView, RenderAttachedBlockView, BiomeColorView {
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

    // (Local Section -> Block Entity Attachment) table.
    private final @Nullable Int2ReferenceMap<Object>[] blockEntityAttachmentArrays;

    // The starting point from which this slice captures blocks
    private int originX, originY, originZ;

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
        this.blockEntityAttachmentArrays = new Int2ReferenceMap[SECTION_ARRAY_SIZE];

        this.biomeSlice = new BiomeSlice();
        this.biomeColors = new BiomeColorCache(this.biomeSlice, MinecraftClient.getInstance().options.getBiomeBlendRadius().getValue());
    }

    public void copyData(ChunkRenderContext context) {
        this.originX = (context.getOrigin().getX() - NEIGHBOR_CHUNK_RADIUS) << 4;
        this.originY = (context.getOrigin().getY() - NEIGHBOR_CHUNK_RADIUS) << 4;
        this.originZ = (context.getOrigin().getZ() - NEIGHBOR_CHUNK_RADIUS) << 4;

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
        this.blockEntityAttachmentArrays[sectionIndex] = section.getBlockEntityAttachmentMap();
    }

    private void unpackBlockData(BlockState[] blockArray, ChunkRenderContext context, ClonedChunkSection section) {
        if (section.getBlockData() == null) {
            this.unpackBlockDataEmpty(blockArray);
        } else if (context.getOrigin().equals(section.getPosition()))  {
            this.unpackBlockDataWhole(blockArray, section);
        } else {
            this.unpackBlockDataPartial(blockArray, section, context.getVolume());
        }
    }

    private void unpackBlockDataEmpty(BlockState[] blockArray) {
        Arrays.fill(blockArray, Blocks.AIR.getDefaultState());
    }

    private void unpackBlockDataPartial(BlockState[] states, ClonedChunkSection section, BlockBox box) {
        PackedIntegerArray array = section.getBlockData();
        Objects.requireNonNull(array);

        ClonedPalette<BlockState> palette = section.getBlockPalette();
        Objects.requireNonNull(palette);

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
                    int localBlockIndex = getLocalBlockIndex(x & 15, y & 15, z & 15);

                    int paletteIndex = array.get(localBlockIndex);
                    var paletteValue =  palette.get(paletteIndex);

                    if (paletteValue == null) {
                        throw new IllegalStateException("Palette does not contain entry: " + paletteIndex);
                    }

                    states[localBlockIndex] = paletteValue;
                }
            }
        }
    }

    private void unpackBlockDataWhole(BlockState[] states, ClonedChunkSection section) {
        ((PackedIntegerArrayExtended) section.getBlockData())
                .sodium$unpack(states, section.getBlockPalette());
    }

    public void reset() {
        // erase any pointers to resources we no longer need
        // no point in cleaning the pre-allocated arrays (such as block state storage) since we hold the
        // only reference.
        for (int sectionIndex = 0; sectionIndex < SECTION_ARRAY_LENGTH; sectionIndex++) {
            Arrays.fill(this.lightArrays[sectionIndex], null);

            this.blockEntityArrays[sectionIndex] = null;
            this.blockEntityAttachmentArrays[sectionIndex] = null;
        }
    }

    @Override
    public BlockState getBlockState(BlockPos pos) {
        return this.getBlockState(pos.getX(), pos.getY(), pos.getZ());
    }

    public BlockState getBlockState(int x, int y, int z) {
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
    public @Nullable Object getBlockEntityRenderAttachment(BlockPos pos) {
        int relX = pos.getX() - this.originX;
        int relY = pos.getY() - this.originY;
        int relZ = pos.getZ() - this.originZ;

        var blockEntityAttachments = this.blockEntityAttachmentArrays[getLocalSectionIndex(relX >> 4, relY >> 4, relZ >> 4)];

        if (blockEntityAttachments == null) {
            return null;
        }

        return blockEntityAttachments.get(getLocalBlockIndex(relX & 15, relY & 15, relZ & 15));
    }

    @Override
    public int getColor(BiomeColorSource source, int x, int y, int z) {
        return this.biomeColors.getColor(source, x, y, z);
    }

    public static int getLocalBlockIndex(int x, int y, int z) {
        return (y << LOCAL_XYZ_BITS << LOCAL_XYZ_BITS) | (z << LOCAL_XYZ_BITS) | x;
    }

    public static int getLocalSectionIndex(int x, int y, int z) {
        return (y * SECTION_ARRAY_LENGTH * SECTION_ARRAY_LENGTH) + (z * SECTION_ARRAY_LENGTH) + x;
    }
}
