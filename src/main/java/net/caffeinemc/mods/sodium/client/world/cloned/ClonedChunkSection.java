package net.caffeinemc.mods.sodium.client.world.cloned;

import it.unimi.dsi.fastutil.ints.Int2ReferenceMap;
import it.unimi.dsi.fastutil.ints.Int2ReferenceMaps;
import it.unimi.dsi.fastutil.ints.Int2ReferenceOpenHashMap;
import net.caffeinemc.mods.sodium.client.world.PalettedContainerROExtension;
import net.caffeinemc.mods.sodium.client.world.LevelSlice;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.DataLayer;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.PalettedContainer;
import net.minecraft.world.level.chunk.PalettedContainerRO;
import net.minecraft.world.level.levelgen.DebugLevelSource;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

/**
 * An immutable clone of a {@link LevelChunkSection}. This contains a full copy of all data (blocks, biomes, light,
 * entities) and can be used by other threads to access the world state.
 */
public class ClonedChunkSection {
    // These default arrays are used to avoid unnecessary copies for empty air sections.
    private static final DataLayer DEFAULT_SKY_LIGHT_ARRAY = new DataLayer(15);
    private static final DataLayer DEFAULT_BLOCK_LIGHT_ARRAY = new DataLayer(0);
    private static final PalettedContainer<BlockState> DEFAULT_STATE_CONTAINER =
            new PalettedContainer<>(Block.BLOCK_STATE_REGISTRY, Blocks.AIR.defaultBlockState(), PalettedContainer.Strategy.SECTION_STATES);

    private final SectionPos pos;

    // Local Block Index -> Block Entity
    private final @Nullable Int2ReferenceMap<BlockEntity> blockEntityMap;

    // Local Block Index -> Block Entity Render Data
    private final @Nullable Int2ReferenceMap<Object> blockEntityRenderDataMap;

    private final @Nullable DataLayer[] lightDataArrays;

    private final @Nullable PalettedContainerRO<BlockState> blockData;

    private final @Nullable PalettedContainerRO<Holder<Biome>> biomeData;

    private long lastUsedTimestamp = Long.MAX_VALUE;

    /**
     * Creates an immutable copy of the chunk section.
     *
     * <p>NOTE: The chunk section's position is provided separately as {@param section} may be null.</p>
     *
     * @param pos     The position of the chunk section to be copied from
     * @param level   The level which the section belongs to
     * @param chunk   The chunk which the section belongs to
     * @param section The chunk section to copy from, which may be null
     */
    public ClonedChunkSection(SectionPos pos, Level level, LevelChunk chunk, @Nullable LevelChunkSection section) {
        this.pos = pos;

        PalettedContainerRO<BlockState> blockData = null;
        PalettedContainerRO<Holder<Biome>> biomeData = null;

        Int2ReferenceMap<BlockEntity> blockEntityMap = null;
        Int2ReferenceMap<Object> blockEntityRenderDataMap = null;

        if (section != null) {
            if (!section.hasOnlyAir()) {
                if (!level.isDebug()) {
                    blockData = PalettedContainerROExtension.clone(section.getStates());
                } else {
                    blockData = constructDebugWorldContainer(pos);
                }
                blockEntityMap = copyBlockEntities(chunk, pos);

                if (blockEntityMap != null) {
                    blockEntityRenderDataMap = copyBlockEntityRenderData(blockEntityMap);
                }
            }

            biomeData = PalettedContainerROExtension.clone(section.getBiomes());
        }

        this.blockData = blockData;
        this.biomeData = biomeData;

        this.blockEntityMap = blockEntityMap;
        this.blockEntityRenderDataMap = blockEntityRenderDataMap;

        this.lightDataArrays = copyLightData(level, pos);
    }

    /**
     * Construct a fake PalettedContainer whose contents match those of the debug world. This is needed to
     * match vanilla's odd approach of short-circuiting getBlockState calls inside its render region class.
     */
    @NotNull
    private static PalettedContainer<BlockState> constructDebugWorldContainer(SectionPos pos) {
        // Fast path for sections which are guaranteed to be empty
        if (pos.getY() != 3 && pos.getY() != 4)
            return DEFAULT_STATE_CONTAINER;

        // We use swapUnsafe in the loops to avoid acquiring/releasing the lock on each iteration
        var container = new PalettedContainer<>(Block.BLOCK_STATE_REGISTRY, Blocks.AIR.defaultBlockState(), PalettedContainer.Strategy.SECTION_STATES);
        if (pos.getY() == 3) {
            // Set the blocks at relative Y 12 (world Y 60) to barriers
            BlockState barrier = Blocks.BARRIER.defaultBlockState();
            for (int z = 0; z < 16; z++) {
                for (int x = 0; x < 16; x++) {
                    container.getAndSetUnchecked(x, 12, z, barrier);
                }
            }
        } else if (pos.getY() == 4) {
            // Set the blocks at relative Y 6 (world Y 70) to the appropriate state from the generator
            for (int z = 0; z < 16; z++) {
                for (int x = 0; x < 16; x++) {
                    container.getAndSetUnchecked(x, 6, z, DebugLevelSource.getBlockStateFor(SectionPos.sectionToBlockCoord(pos.getX(), x), SectionPos.sectionToBlockCoord(pos.getZ(), z)));
                }
            }
        }
        return container;
    }

    /**
     * Copies the light data for the given chunk section from the level. If the level's dimension does not support
     * a given light type, the array for that type will be null.
     *
     * @param level The level to copy from
     * @param pos The coordinates of the chunk section to copy from
     * @return The copied light arrays
     */
    private static DataLayer[] copyLightData(Level level, SectionPos pos) {
        var arrays = new DataLayer[2];
        arrays[LightLayer.BLOCK.ordinal()] = copyLightArray(level, LightLayer.BLOCK, pos);

        // Dimensions without sky-light should not have a default-initialized array
        if (level.dimensionType().hasSkyLight()) {
            arrays[LightLayer.SKY.ordinal()] = copyLightArray(level, LightLayer.SKY, pos);
        }

        return arrays;
    }

    /**
     * Copies the light data array for the given light type for this chunk, or returns a default-initialized value if
     * the light array is not loaded.
     *
     * @param level The level to copy from
     * @param pos The coordinates of the chunk section to copy from
     * @return The copied light array
     */
    @NotNull
    private static DataLayer copyLightArray(Level level, LightLayer type, SectionPos pos) {
        var array = level.getLightEngine()
                .getLayerListener(type)
                .getDataLayerData(pos);

        if (array == null) {
            array = switch (type) {
                case SKY -> DEFAULT_SKY_LIGHT_ARRAY;
                case BLOCK -> DEFAULT_BLOCK_LIGHT_ARRAY;
            };
        }

        return array;
    }

    /**
     * Copies the references to any block entities located within the chunk. This does not make a deep copy of the
     * block entities themselves, so it is generally dangerous to access their state.
     *
     * @param chunk The chunk to copy entities from
     * @param sectionPos The coordinates of the chunk section to copy from
     * @return The copied block entities, or null if there are no entities to copy
     */
    @Nullable
    private static Int2ReferenceMap<BlockEntity> copyBlockEntities(LevelChunk chunk, SectionPos sectionPos) {
        BoundingBox box = new BoundingBox(sectionPos.minBlockX(), sectionPos.minBlockY(), sectionPos.minBlockZ(),
                sectionPos.maxBlockX(), sectionPos.maxBlockY(), sectionPos.maxBlockZ());

        Int2ReferenceOpenHashMap<BlockEntity> blockEntities = null;

        // Copy the block entities from the chunk into our cloned section
        for (Map.Entry<BlockPos, BlockEntity> entry : chunk.getBlockEntities().entrySet()) {
            BlockPos pos = entry.getKey();
            BlockEntity entity = entry.getValue();

            if (box.isInside(pos)) {
                if (blockEntities == null) {
                    blockEntities = new Int2ReferenceOpenHashMap<>();
                }

                blockEntities.put(LevelSlice.getLocalBlockIndex(pos.getX() & 15, pos.getY() & 15, pos.getZ() & 15), entity);
            }
        }

        if (blockEntities != null) {
            blockEntities.trim();
        }

        return blockEntities;
    }

    /**
     * Copies the references to render attachment data on block entities. This does not make a deep copy of the
     * render attachment data itself.
     * @param blockEntities The block entities to copy render attachment data from
     * @return The copied render attachment references, or null if there is none to copy
     */
    @Nullable
    private static Int2ReferenceMap<Object> copyBlockEntityRenderData(Int2ReferenceMap<BlockEntity> blockEntities) {
        Int2ReferenceOpenHashMap<Object> blockEntityRenderDataMap = null;

        // Retrieve any render data after we have copied all block entities, as this will call into the code of
        // other mods. This could potentially result in the chunk being modified, which would cause problems if we
        // were iterating over any data in that chunk.
        // See https://github.com/CaffeineMC/sodium-fabric/issues/942 for more info.
        for (var entry : Int2ReferenceMaps.fastIterable(blockEntities)) {
            Object data = entry.getValue().getRenderData();

            if (data != null) {
                if (blockEntityRenderDataMap == null) {
                    blockEntityRenderDataMap = new Int2ReferenceOpenHashMap<>();
                }

                blockEntityRenderDataMap.put(entry.getIntKey(), data);
            }
        }

        if (blockEntityRenderDataMap != null) {
            blockEntityRenderDataMap.trim();
        }

        return blockEntityRenderDataMap;
    }

    /**
     * @return The position of the section which this been cloned
     */
    public SectionPos getPosition() {
        return this.pos;
    }

    public @Nullable PalettedContainerRO<BlockState> getBlockData() {
        return this.blockData;
    }

    public @Nullable PalettedContainerRO<Holder<Biome>> getBiomeData() {
        return this.biomeData;
    }

    public @Nullable Int2ReferenceMap<BlockEntity> getBlockEntityMap() {
        return this.blockEntityMap;
    }

    public @Nullable Int2ReferenceMap<Object> getBlockEntityRenderDataMap() {
        return this.blockEntityRenderDataMap;
    }

    public @Nullable DataLayer getLightArray(LightLayer lightType) {
        return this.lightDataArrays[lightType.ordinal()];
    }

    public long getLastUsedTimestamp() {
        return this.lastUsedTimestamp;
    }

    public void setLastUsedTimestamp(long timestamp) {
        this.lastUsedTimestamp = timestamp;
    }
}
