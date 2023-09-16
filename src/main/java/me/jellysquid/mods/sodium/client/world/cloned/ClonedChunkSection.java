package me.jellysquid.mods.sodium.client.world.cloned;

import it.unimi.dsi.fastutil.ints.Int2ReferenceMap;
import it.unimi.dsi.fastutil.ints.Int2ReferenceMaps;
import it.unimi.dsi.fastutil.ints.Int2ReferenceOpenHashMap;
import me.jellysquid.mods.sodium.client.world.ReadableContainerExtended;
import me.jellysquid.mods.sodium.client.world.WorldSlice;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.world.LightType;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.ChunkNibbleArray;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.ReadableContainer;
import net.minecraft.world.chunk.WorldChunk;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public class ClonedChunkSection {
    private static final ChunkNibbleArray DEFAULT_SKY_LIGHT_ARRAY = new ChunkNibbleArray(15);
    private static final ChunkNibbleArray DEFAULT_BLOCK_LIGHT_ARRAY = new ChunkNibbleArray(0);

    private final ChunkSectionPos pos;

    private final @Nullable Int2ReferenceMap<BlockEntity> blockEntityMap;
    private final @Nullable Int2ReferenceMap<Object> blockEntityRenderDataMap;

    private final @Nullable ChunkNibbleArray[] lightDataArrays;

    private final @Nullable ReadableContainer<BlockState> blockData;

    private final @Nullable ReadableContainer<RegistryEntry<Biome>> biomeData;

    private long lastUsedTimestamp = Long.MAX_VALUE;

    public ClonedChunkSection(World world, WorldChunk chunk, @Nullable ChunkSection section, ChunkSectionPos pos) {
        this.pos = pos;

        ReadableContainer<BlockState> blockData = null;
        ReadableContainer<RegistryEntry<Biome>> biomeData = null;

        Int2ReferenceMap<BlockEntity> blockEntityMap = null;
        Int2ReferenceMap<Object> blockEntityRenderDataMap = null;

        if (section != null) {
            if (!section.isEmpty()) {
                blockData = ReadableContainerExtended.clone(section.getBlockStateContainer());
                blockEntityMap = copyBlockEntities(chunk, pos);

                if (blockEntityMap != null) {
                    blockEntityRenderDataMap = copyBlockEntityRenderData(blockEntityMap);
                }
            }

            biomeData = ReadableContainerExtended.clone(section.getBiomeContainer());
        }

        this.blockData = blockData;
        this.biomeData = biomeData;

        this.blockEntityMap = blockEntityMap;
        this.blockEntityRenderDataMap = blockEntityRenderDataMap;

        this.lightDataArrays = copyLightData(world, pos);
    }

    @NotNull
    private static ChunkNibbleArray[] copyLightData(World world, ChunkSectionPos pos) {
        var arrays = new ChunkNibbleArray[2];
        arrays[LightType.BLOCK.ordinal()] = copyLightArray(world, LightType.BLOCK, pos);

        // Dimensions without sky-light should not have a default-initialized array
        if (world.getDimension().hasSkyLight()) {
            arrays[LightType.SKY.ordinal()] = copyLightArray(world, LightType.SKY, pos);
        }

        return arrays;
    }

    /**
     * Copies the light data array for the given light type for this chunk, or returns a default-initialized value if
     * the light array is not loaded.
     */
    @NotNull
    private static ChunkNibbleArray copyLightArray(World world, LightType type, ChunkSectionPos pos) {
        var array = world.getLightingProvider()
                .get(type)
                .getLightSection(pos);

        if (array == null) {
            array = switch (type) {
                case SKY -> DEFAULT_SKY_LIGHT_ARRAY;
                case BLOCK -> DEFAULT_BLOCK_LIGHT_ARRAY;
            };
        }

        return array;
    }

    @Nullable
    private static Int2ReferenceMap<BlockEntity> copyBlockEntities(WorldChunk chunk, ChunkSectionPos chunkCoord) {
        BlockBox box = new BlockBox(chunkCoord.getMinX(), chunkCoord.getMinY(), chunkCoord.getMinZ(),
                chunkCoord.getMaxX(), chunkCoord.getMaxY(), chunkCoord.getMaxZ());

        Int2ReferenceOpenHashMap<BlockEntity> blockEntities = null;

        // Copy the block entities from the chunk into our cloned section
        for (Map.Entry<BlockPos, BlockEntity> entry : chunk.getBlockEntities().entrySet()) {
            BlockPos pos = entry.getKey();
            BlockEntity entity = entry.getValue();

            if (box.contains(pos)) {
                if (blockEntities == null) {
                    blockEntities = new Int2ReferenceOpenHashMap<>();
                }

                blockEntities.put(WorldSlice.getLocalBlockIndex(pos.getX() & 15, pos.getY() & 15, pos.getZ() & 15), entity);
            }
        }

        if (blockEntities != null) {
            blockEntities.trim();
        }

        return blockEntities;
    }

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

    public ChunkSectionPos getPosition() {
        return this.pos;
    }

    public @Nullable ReadableContainer<BlockState> getBlockData() {
        return this.blockData;
    }

    public @Nullable ReadableContainer<RegistryEntry<Biome>> getBiomeData() {
        return this.biomeData;
    }

    public @Nullable Int2ReferenceMap<BlockEntity> getBlockEntityMap() {
        return this.blockEntityMap;
    }

    public @Nullable Int2ReferenceMap<Object> getBlockEntityRenderDataMap() {
        return this.blockEntityRenderDataMap;
    }

    public @Nullable ChunkNibbleArray getLightArray(LightType lightType) {
        return this.lightDataArrays[lightType.ordinal()];
    }

    public long getLastUsedTimestamp() {
        return this.lastUsedTimestamp;
    }

    public void setLastUsedTimestamp(long timestamp) {
        this.lastUsedTimestamp = timestamp;
    }
}
