package me.jellysquid.mods.sodium.client.world.cloned;

import it.unimi.dsi.fastutil.shorts.Short2ShortOpenHashMap;
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
import org.apache.commons.lang3.Validate;

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class ClonedChunkSection {
    private static final LightType[] LIGHT_TYPES = LightType.values();

    private final BlockEntity[] blockEntity = new BlockEntity[16 * 16 * 16];
    private final Object[] blockEntityAttachments = new Object[16 * 16 * 16];

    private final Short2ShortOpenHashMap blockEntityIds;
    private final ChunkNibbleArray[] lightDataArrays;

    private ChunkSectionPos pos;

    private PackedIntegerArray blockStateData;
    private ClonedPalette<BlockState> blockStatePalette;

    private ReadableContainer<RegistryEntry<Biome>> biomeData;

    private boolean empty = true;

    ClonedChunkSection() {
        this.blockEntityIds = new Short2ShortOpenHashMap();
        this.blockEntityIds.defaultReturnValue((short) -1);

        this.lightDataArrays = new ChunkNibbleArray[LIGHT_TYPES.length];
    }

    public void copy(World world, WorldChunk chunk, ChunkSection section, ChunkSectionPos pos) {
        Validate.isTrue(this.empty);

        this.pos = pos;

        this.copyBlockData(section);
        this.copyLightData(world);
        this.copyBiomeData(section);
        this.copyBlockEntities(chunk, pos);
    }

    void clear() {
        Arrays.fill(this.blockEntity, 0, this.blockEntityIds.size(), null);
        Arrays.fill(this.blockEntityAttachments, 0, this.blockEntityIds.size(), null);

        this.blockEntityIds.clear();

        this.blockStateData = null;
        this.blockStatePalette = null;

        this.biomeData = null;

        Arrays.fill(this.lightDataArrays, null);

        this.empty = true;
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

    public ChunkNibbleArray getLightArray(LightType type) {
        return this.lightDataArrays[type.ordinal()];
    }

    private void copyBlockEntities(WorldChunk chunk, ChunkSectionPos chunkCoord) {
        BlockBox box = new BlockBox(chunkCoord.getMinX(), chunkCoord.getMinY(), chunkCoord.getMinZ(),
                chunkCoord.getMaxX(), chunkCoord.getMaxY(), chunkCoord.getMaxZ());

        // Copy the block entities from the chunk into our cloned section
        for (Map.Entry<BlockPos, BlockEntity> entry : chunk.getBlockEntities().entrySet()) {
            BlockPos pos = entry.getKey();
            BlockEntity entity = entry.getValue();

            if (box.contains(pos)) {
                var id = (short) this.blockEntityIds.size();
                var prev = this.blockEntityIds.put(ChunkSectionPos.packLocal(pos), id);

                if (prev != this.blockEntityIds.defaultReturnValue()) {
                    throw new IllegalStateException("Already inserted block entity at " + pos);
                }

                this.blockEntity[id] = entity;
            }
        }

        // Retrieve any render attachments after we have copied all block entities, as this will call into the code of
        // other mods. This could potentially result in the chunk being modified, which would cause problems if we
        // were iterating over any data in that chunk.
        // See https://github.com/CaffeineMC/sodium-fabric/issues/942 for more info.
        for (int i = 0; i < this.blockEntityIds.size(); i++) {
            if (this.blockEntity[i] instanceof RenderAttachmentBlockEntity holder) {
                this.blockEntityAttachments[i] = holder.getRenderAttachmentData();
            }
        }
    }

    public RegistryEntry<Biome> getBiome(int x, int y, int z) {
        return this.biomeData.get(x, y, z);
    }

    public BlockEntity getBlockEntity(int x, int y, int z) {
        var id = this.blockEntityIds.get(packLocal(x, y, z));

        if (id < 0) {
            return null;
        }

        return this.blockEntity[id];
    }

    public Object getBlockEntityRenderAttachment(int x, int y, int z) {
        var id = this.blockEntityIds.get(packLocal(x, y, z));

        if (id < 0) {
            return null;
        }

        return this.blockEntityAttachments[id];
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

    public int getLightLevel(LightType type, int x, int y, int z) {
        var array = this.lightDataArrays[type.ordinal()];

        // The sky-light array may not exist in certain dimensions.
        if (array == null) {
            return 0;
        }

        return array.get(x, y, z);
    }

    private final AtomicInteger referenceCount = new AtomicInteger(0);

    private long lastUsedTimestamp;

    public int getReferenceCount() {
        return this.referenceCount.get();
    }

    public long getLastUsedTimestamp() {
        return this.lastUsedTimestamp;
    }

    public void setLastUsedTimestamp(long timestamp) {
        this.lastUsedTimestamp = timestamp;
    }

    public void acquireReference() {
        this.referenceCount.getAndIncrement();
    }

    public void releaseReference() {
        this.referenceCount.getAndDecrement();
    }
}
