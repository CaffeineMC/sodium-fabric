package me.jellysquid.mods.sodium.client.world.cloned;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import me.jellysquid.mods.sodium.client.world.cloned.palette.ClonedPalette;
import me.jellysquid.mods.sodium.client.world.cloned.palette.ClonedPaletteFallback;
import me.jellysquid.mods.sodium.client.world.cloned.palette.ClonedPalleteArray;
import net.fabricmc.fabric.api.rendering.data.v1.RenderAttachmentBlockEntity;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.util.collection.PackedIntegerArray;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.world.LightType;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.source.BiomeArray;
import net.minecraft.world.chunk.*;

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class ClonedChunkSection {
    private static final LightType[] LIGHT_TYPES = LightType.values();
    private static final ChunkSection EMPTY_SECTION = new ChunkSection(0);

    private final AtomicInteger referenceCount = new AtomicInteger(0);
    private final ClonedChunkSectionCache backingCache;

    private final Long2ObjectOpenHashMap<BlockEntity> blockEntities;
    private final Long2ObjectOpenHashMap<Object> renderAttachments;

    private final ChunkNibbleArray[] lightDataArrays;

    private ChunkSectionPos pos;

    private PackedIntegerArray blockStateData;
    private ClonedPalette<BlockState> blockStatePalette;

    private BiomeArray biomeData;

    ClonedChunkSection(ClonedChunkSectionCache backingCache) {
        this.backingCache = backingCache;
        this.blockEntities = new Long2ObjectOpenHashMap<>();
        this.renderAttachments = new Long2ObjectOpenHashMap<>();
        this.lightDataArrays = new ChunkNibbleArray[LIGHT_TYPES.length];
    }

    public void init(World world, ChunkSectionPos pos) {
        WorldChunk chunk = world.getChunk(pos.getX(), pos.getZ());

        if (chunk == null) {
            throw new RuntimeException("Couldn't retrieve chunk at " + pos.toChunkPos());
        }

        ChunkSection section = getChunkSection(world, chunk, pos);

        if (ChunkSection.isEmpty(section)) {
            section = EMPTY_SECTION;
        }

        this.reset(pos);

        this.copyBlockData(section);
        this.copyLightData(world);
        this.copyBiomeData(chunk);
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
        PalettedContainerExtended<BlockState> container = PalettedContainerExtended.cast(section.getContainer());

        this.blockStateData = copyBlockData(container);
        this.blockStatePalette = copyPalette(container);
    }

    private void copyLightData(World world) {
        for (LightType type : LIGHT_TYPES) {
            this.lightDataArrays[type.ordinal()] = world.getLightingProvider()
                    .get(type)
                    .getLightSection(pos);
        }
    }

    private void copyBiomeData(Chunk chunk) {
        this.biomeData = chunk.getBiomeArray();
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

        this.blockEntities.clear();

        for (Map.Entry<BlockPos, BlockEntity> entry : chunk.getBlockEntities().entrySet()) {
            BlockPos pos = entry.getKey();
            BlockEntity entity = entry.getValue();

            if (box.contains(pos)) {
                this.addBlockEntity(pos, entity);
            }
        }
    }

    private void addBlockEntity(BlockPos pos, BlockEntity entity) {
        long key = BlockPos.asLong(pos.getX() & 15, pos.getY() & 15, pos.getZ() & 15);

        this.blockEntities.put(key, entity);

        if (entity instanceof RenderAttachmentBlockEntity) {
            this.renderAttachments.put(key, ((RenderAttachmentBlockEntity) entity).getRenderAttachmentData());
        }
    }

    public Biome getBiomeForNoiseGen(int x, int y, int z) {
        return this.biomeData.getBiomeForNoiseGen(x, y, z);
    }

    public BlockEntity getBlockEntity(int x, int y, int z) {
        return this.blockEntities.get(BlockPos.asLong(x, y, z));
    }

    public Object getBlockEntityRenderAttachment(int x, int y, int z) {
        return this.renderAttachments.get(BlockPos.asLong(x, y, z));
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

    private static ClonedPalette<BlockState> copyPalette(PalettedContainerExtended<BlockState> container) {
        Palette<BlockState> palette = container.getPalette();

        if (palette instanceof IdListPalette) {
            return new ClonedPaletteFallback<>(Block.STATE_IDS);
        }

        BlockState[] array = new BlockState[1 << container.getPaletteSize()];

        for (int i = 0; i < array.length; i++) {
            array[i] = palette.getByIndex(i);

            if (array[i] == null) {
                break;
            }
        }

        return new ClonedPalleteArray<>(array, container.getDefaultValue());
    }

    private static PackedIntegerArray copyBlockData(PalettedContainerExtended<BlockState> container) {
        PackedIntegerArray array = container.getDataArray();
        long[] storage = array.getStorage();

        return new PackedIntegerArray(container.getPaletteSize(), array.getSize(), storage.clone());
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
}
