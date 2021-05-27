package me.jellysquid.mods.sodium.client.world.cloned;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import me.jellysquid.mods.sodium.client.world.ClientWorldExtended;
import me.jellysquid.mods.sodium.client.world.WorldSlice;
import me.jellysquid.mods.sodium.client.world.biome.BiomeCache;
import me.jellysquid.mods.sodium.client.world.cloned.palette.ClonedPalette;
import me.jellysquid.mods.sodium.client.world.cloned.palette.ClonedPaletteFallback;
import me.jellysquid.mods.sodium.client.world.cloned.palette.ClonedPalleteArray;
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

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class ClonedChunkSection {
    private static final LightType[] LIGHT_TYPES = LightType.values();
    private static final ChunkSection EMPTY_SECTION = new ChunkSection(0);

    private final AtomicInteger referenceCount = new AtomicInteger(0);
    private final ClonedChunkSectionCache backingCache;

    private final Long2ObjectOpenHashMap<BlockEntity> blockEntities;
    private final ChunkNibbleArray[] lightDataArrays;
    private final World world;

    private ChunkSectionPos pos;

    private PackedIntegerArray blockStateData;
    private ClonedPalette<BlockState> blockStatePalette;

    private BiomeArray biomeData;

    ClonedChunkSection(ClonedChunkSectionCache backingCache, World world) {
        this.backingCache = backingCache;
        this.world = world;
        this.blockEntities = new Long2ObjectOpenHashMap<>(8);
        this.lightDataArrays = new ChunkNibbleArray[LIGHT_TYPES.length];
    }

    public void init(ChunkSectionPos pos) {
        WorldChunk chunk = world.getChunk(pos.getX(), pos.getZ());

        if (chunk == null) {
            throw new RuntimeException("Couldn't retrieve chunk at " + pos.toChunkPos());
        }

        ChunkSection section = getChunkSection(chunk, pos);

        if (ChunkSection.isEmpty(section)) {
            section = EMPTY_SECTION;
        }

        this.pos = pos;

        PalettedContainerExtended<BlockState> container = PalettedContainerExtended.cast(section.getContainer());;

        this.blockStateData = copyBlockData(container);
        this.blockStatePalette = copyPalette(container);

        for (LightType type : LIGHT_TYPES) {
            this.lightDataArrays[type.ordinal()] = world.getLightingProvider()
                    .get(type)
                    .getLightSection(pos);
        }

        this.biomeData = chunk.getBiomeArray();

        BlockBox box = new BlockBox(pos.getMinX(), pos.getMinY(), pos.getMinZ(), pos.getMaxX(), pos.getMaxY(), pos.getMaxZ());

        this.blockEntities.clear();

        for (Map.Entry<BlockPos, BlockEntity> entry : chunk.getBlockEntities().entrySet()) {
            BlockPos entityPos = entry.getKey();

            if (box.contains(entityPos)) {
                this.blockEntities.put(BlockPos.asLong(entityPos.getX() & 15, entityPos.getY() & 15, entityPos.getZ() & 15), entry.getValue());
            }
        }
    }

    public BlockState getBlockState(int x, int y, int z) {
        return this.blockStatePalette.get(this.blockStateData.get(y << 8 | z << 4 | x));
    }

    public int getLightLevel(LightType type, int x, int y, int z) {
        ChunkNibbleArray array = this.lightDataArrays[type.ordinal()];

        if (array != null) {
            return array.get(x, y, z);
        }

        return 0;
    }

    public Biome getBiomeForNoiseGen(int x, int y, int z) {
        return this.biomeData.getBiomeForNoiseGen(x, y, z);
    }

    public BlockEntity getBlockEntity(int x, int y, int z) {
        return this.blockEntities.get(BlockPos.asLong(x, y, z));
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

    private static ChunkSection getChunkSection(Chunk chunk, ChunkSectionPos pos) {
        ChunkSection section = null;

        if (!World.isOutOfBuildLimitVertically(ChunkSectionPos.getBlockCoord(pos.getY()))) {
            section = chunk.getSectionArray()[pos.getY()];
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
