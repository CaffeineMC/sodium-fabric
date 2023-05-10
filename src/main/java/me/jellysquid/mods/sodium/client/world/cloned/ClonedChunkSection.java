package me.jellysquid.mods.sodium.client.world.cloned;

import it.unimi.dsi.fastutil.shorts.Short2ObjectMap;
import it.unimi.dsi.fastutil.shorts.Short2ObjectMaps;
import it.unimi.dsi.fastutil.shorts.Short2ObjectOpenHashMap;
import me.jellysquid.mods.sodium.client.world.cloned.palette.ClonedPalette;
import me.jellysquid.mods.sodium.client.world.cloned.palette.ClonedPaletteFallback;
import me.jellysquid.mods.sodium.client.world.cloned.palette.ClonedPalleteArray;
import net.fabricmc.fabric.api.rendering.data.v1.RenderAttachmentBlockEntity;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.registry.BuiltinRegistries;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.collection.PackedIntegerArray;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.registry.Registry;
import net.minecraft.world.LightType;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeKeys;
import net.minecraft.world.chunk.*;

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class ClonedChunkSection {
    private static final LightType[] LIGHT_TYPES = LightType.values();
    private ChunkSection EMPTY_SECTION;
    private static final PalettedContainer<BlockState> EMPTY_CONTAINER = new PalettedContainer(Block.STATE_IDS, Blocks.AIR.getDefaultState(), PalettedContainer.PaletteProvider.BLOCK_STATE);

    private final AtomicInteger referenceCount = new AtomicInteger(0);
    private final ClonedChunkSectionCache backingCache;

    private final Short2ObjectMap<BlockEntity> blockEntities;
    private final Short2ObjectMap<Object> renderAttachments;

    private final ChunkNibbleArray[] lightDataArrays;

    private ChunkSectionPos pos;

    private PackedIntegerArray blockStateData;
    private ClonedPalette<BlockState> blockStatePalette;

    private ReadableContainer<RegistryEntry<Biome>> biomeData;

    ClonedChunkSection(ClonedChunkSectionCache backingCache) {
        this.backingCache = backingCache;
        this.blockEntities = new Short2ObjectOpenHashMap<>();
        this.renderAttachments = new Short2ObjectOpenHashMap<>();
        this.lightDataArrays = new ChunkNibbleArray[LIGHT_TYPES.length];
    }

    public void init(World world, ChunkSectionPos pos) {
        EMPTY_SECTION =  new ChunkSection(EMPTY_CONTAINER, new PalettedContainer<RegistryEntry<Biome>>(world.getRegistryManager().get(RegistryKeys.BIOME).getIndexedEntries(), world.getRegistryManager().get(RegistryKeys.BIOME).entryOf(BiomeKeys.PLAINS), PalettedContainer.PaletteProvider.BIOME));

        WorldChunk chunk = world.getChunk(pos.getX(), pos.getZ());

        if (chunk == null) {
            throw new RuntimeException("Couldn't retrieve chunk at " + pos.toChunkPos());
        }

        ChunkSection section = getChunkSection(world, chunk, pos);

        if (section == null) {
            section = EMPTY_SECTION;
        }

        this.reset(pos);

        this.copyBlockData(section);
        this.copyLightData(world);
        this.copyBiomeData(section);
        this.copyBlockEntities(chunk, pos);
    }

    private void reset(ChunkSectionPos pos) {
        this.pos = pos;
        this.blockEntities.clear();
        this.renderAttachments.clear();

        this.blockStateData = null;
        this.blockStatePalette = null;

        this.biomeData = null;

        Arrays.fill(this.lightDataArrays, null);
    }

    private void copyBlockData(ChunkSection section) {
        PalettedContainer.Data<BlockState> container = PalettedContainerAccessor.getData(section.getBlockStateContainer());

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

    public int getLightLevel(LightType type, int x, int y, int z) {
        ChunkNibbleArray array = this.lightDataArrays[type.ordinal()];

        if (array != null) {
            return array.get(x, y, z);
        }

        return 0;
    }

    private void copyBlockEntities(WorldChunk chunk, ChunkSectionPos chunkCoord) {
        BlockBox box = new BlockBox(chunkCoord.getMinX(), chunkCoord.getMinY(), chunkCoord.getMinZ(),
                chunkCoord.getMaxX(), chunkCoord.getMaxY(), chunkCoord.getMaxZ());

        // Copy the block entities from the chunk into our cloned section
        for (Map.Entry<BlockPos, BlockEntity> entry : chunk.getBlockEntities().entrySet()) {
            BlockPos pos = entry.getKey();
            BlockEntity entity = entry.getValue();

            if (box.contains(pos)) {
                this.blockEntities.put(ChunkSectionPos.packLocal(pos), entity);
            }
        }

        // Retrieve any render attachments after we have copied all block entities, as this will call into the code of
        // other mods. This could potentially result in the chunk being modified, which would cause problems if we
        // were iterating over any data in that chunk.
        // See https://github.com/CaffeineMC/sodium-fabric/issues/942 for more info.
        for (Short2ObjectMap.Entry<BlockEntity> entry : Short2ObjectMaps.fastIterable(this.blockEntities)) {
            if (entry.getValue() instanceof RenderAttachmentBlockEntity entity) {
                this.renderAttachments.put(entry.getShortKey(), entity.getRenderAttachmentData());
            }
        }
    }

    public RegistryEntry<Biome> getBiome(int x, int y, int z) {
        return this.biomeData.get(x, y, z);
    }

    public BlockEntity getBlockEntity(int x, int y, int z) {
        return this.blockEntities.get(packLocal(x, y, z));
    }

    public Object getBlockEntityRenderAttachment(int x, int y, int z) {
        return this.renderAttachments.get(packLocal(x, y, z));
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

    private static ChunkSection getChunkSection(World world, Chunk chunk, ChunkSectionPos pos) {
        ChunkSection section = null;

        if (!world.isOutOfHeightLimit(ChunkSectionPos.getBlockCoord(pos.getY()))) {
            section = chunk.getSectionArray()[world.sectionCoordToIndex(pos.getY())];
        }

        return section;
    }

    public void acquireReference() {
        this.referenceCount.incrementAndGet();
    }

    public boolean releaseReference() {
        return this.referenceCount.decrementAndGet() <= 0;
    }

    public ClonedChunkSectionCache getBackingCache() {
        return this.backingCache;
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
}
