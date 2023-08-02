package me.jellysquid.mods.sodium.client.world.cloned;

import it.unimi.dsi.fastutil.ints.Int2ReferenceMap;
import it.unimi.dsi.fastutil.ints.Int2ReferenceMaps;
import it.unimi.dsi.fastutil.ints.Int2ReferenceOpenHashMap;
import me.jellysquid.mods.sodium.client.world.cloned.palette.ClonedPalette;
import me.jellysquid.mods.sodium.client.world.cloned.palette.ClonedPaletteFallback;
import me.jellysquid.mods.sodium.client.world.cloned.palette.ClonedPalleteArray;
import me.jellysquid.mods.sodium.mixin.core.world.chunk.PalettedContainerAccessor;
import net.fabricmc.fabric.api.rendering.data.v1.RenderAttachmentBlockEntity;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.collection.PackedIntegerArray;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.world.LightType;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.*;

import java.util.EnumMap;
import java.util.Map;

public class ClonedChunkSection {
    private static final LightType[] LIGHT_TYPES = LightType.values();

    private Int2ReferenceMap<BlockEntity> blockEntities;
    private Int2ReferenceMap<Object> blockEntityAttachments;

    private final ChunkNibbleArray[] lightDataArrays = new ChunkNibbleArray[LIGHT_TYPES.length];

    private final ChunkSectionPos pos;

    private PackedIntegerArray blockStateData;
    private ClonedPalette<BlockState> blockStatePalette;

    private ReadableContainer<RegistryEntry<Biome>> biomeData;

    private long lastUsedTimestamp = Long.MAX_VALUE;

    public ClonedChunkSection(World world, WorldChunk chunk, ChunkSection section, ChunkSectionPos pos) {
        this.pos = pos;

        this.copyBlockData(section);
        this.copyLightData(world);
        this.copyBiomeData(section);
        this.copyBlockEntities(chunk, pos);
    }

    private void copyBlockData(ChunkSection section) {
        PalettedContainer.Data<BlockState> container = ((PalettedContainerAccessor<BlockState>) section.getBlockStateContainer()).getData();

        this.blockStateData = copyBlockData(container);
        this.blockStatePalette = copyPalette(container);
    }

    private void copyLightData(World world) {
        for (LightType type : LIGHT_TYPES) {
            this.lightDataArrays[type.ordinal()] = world.getLightingProvider()
                    .get(type)
                    .getLightSection(this.pos);
        }
    }

    private void copyBiomeData(ChunkSection section) {
        this.biomeData = section.getBiomeContainer();
    }

    public ChunkNibbleArray[] getLightDataArrays() {
        return this.lightDataArrays;
    }

    private void copyBlockEntities(WorldChunk chunk, ChunkSectionPos chunkCoord) {
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

                blockEntities.put(packLocal(pos.getX() & 15, pos.getY() & 15, pos.getZ() & 15), entity);
            }
        }

        this.blockEntities = blockEntities != null ? blockEntities : Int2ReferenceMaps.emptyMap();

        Int2ReferenceOpenHashMap<Object> blockEntityAttachments = null;

        // Retrieve any render attachments after we have copied all block entities, as this will call into the code of
        // other mods. This could potentially result in the chunk being modified, which would cause problems if we
        // were iterating over any data in that chunk.
        // See https://github.com/CaffeineMC/sodium-fabric/issues/942 for more info.
        for (var entry : Int2ReferenceMaps.fastIterable(this.blockEntities)) {
            if (entry.getValue() instanceof RenderAttachmentBlockEntity holder) {
                if (blockEntityAttachments == null) {
                    blockEntityAttachments = new Int2ReferenceOpenHashMap<>();
                }

                blockEntityAttachments.put(entry.getIntKey(), holder.getRenderAttachmentData());
            }
        }

        this.blockEntityAttachments = blockEntityAttachments != null ? blockEntityAttachments : Int2ReferenceMaps.emptyMap();
    }

    public RegistryEntry<Biome> getBiome(int x, int y, int z) {
        return this.biomeData.get(x, y, z);
    }

    public PackedIntegerArray getBlockData() {
        return this.blockStateData;
    }

    public ClonedPalette<BlockState> getBlockPalette() {
        return this.blockStatePalette;
    }

    public ChunkSectionPos getPosition() {
        return this.pos;
    }

    private static ClonedPalette<BlockState> copyPalette(PalettedContainer.Data<BlockState> container) {
        Palette<BlockState> palette = container.palette();

        if (palette instanceof IdListPalette) {
            return new ClonedPaletteFallback<>(Block.STATE_IDS);
        }

        BlockState[] array = new BlockState[container.palette().getSize()];

        for (int i = 0; i < array.length; i++) {
            array[i] = palette.get(i);
        }

        return new ClonedPalleteArray<>(array);
    }

    private static PackedIntegerArray copyBlockData(PalettedContainer.Data<BlockState> container) {
        var storage = container.storage();
        var data = storage.getData();
        var bits = container.configuration().bits();

        if (bits == 0) {
            // TODO: avoid this allocation
            return new PackedIntegerArray(1, storage.getSize());
        }

        return new PackedIntegerArray(bits, storage.getSize(), data.clone());
    }

    /**
     * @param x The local x-coordinate
     * @param y The local y-coordinate
     * @param z The local z-coordinate
     * @return An index which can be used to key entities or blocks within a chunk
     */
    private static short packLocal(int x, int y, int z) {
        return (short) (x << 8 | z << 4 | y);
    }

    public long getLastUsedTimestamp() {
        return this.lastUsedTimestamp;
    }

    public void setLastUsedTimestamp(long timestamp) {
        this.lastUsedTimestamp = timestamp;
    }

    public Int2ReferenceMap<BlockEntity> getBlockEntityMap() {
        return this.blockEntities;
    }

    public Int2ReferenceMap<Object> getBlockEntityAttachmentMap() {
        return this.blockEntityAttachments;
    }
}
