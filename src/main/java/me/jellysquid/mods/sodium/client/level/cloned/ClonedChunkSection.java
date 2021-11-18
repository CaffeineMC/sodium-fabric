package me.jellysquid.mods.sodium.client.level.cloned;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import me.jellysquid.mods.sodium.client.level.cloned.palette.ClonedPalette;
import me.jellysquid.mods.sodium.client.level.cloned.palette.ClonedPaletteFallback;
import me.jellysquid.mods.sodium.client.level.cloned.palette.ClonedPalleteArray;
import net.fabricmc.fabric.api.rendering.data.v1.RenderAttachmentBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.util.BitStorage;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkBiomeContainer;
import net.minecraft.world.level.chunk.DataLayer;
import net.minecraft.world.level.chunk.GlobalPalette;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.Palette;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class ClonedChunkSection {
    private static final LightLayer[] LIGHT_TYPES = LightLayer.values();
    private static final LevelChunkSection EMPTY_SECTION = new LevelChunkSection(0);

    private final AtomicInteger referenceCount = new AtomicInteger(0);
    private final ClonedChunkSectionCache backingCache;

    private final Short2ObjectMap<BlockEntity> blockEntities;
    private final Short2ObjectMap<Object> renderAttachments;

    private final DataLayer[] lightDataArrays;

    private SectionPos pos;

    private BitStorage blockStateData;
    private ClonedPalette<BlockState> blockStatePalette;

    private ChunkBiomeContainer biomeData;

    ClonedChunkSection(ClonedChunkSectionCache backingCache) {
        this.backingCache = backingCache;
        this.blockEntities = new Short2ObjectOpenHashMap<>();
        this.renderAttachments = new Short2ObjectOpenHashMap<>();
        this.lightDataArrays = new DataLayer[LIGHT_TYPES.length];
    }

    public void init(Level level, SectionPos pos) {
        LevelChunk chunk = level.getChunk(pos.getX(), pos.getZ());

        if (chunk == null) {
            throw new RuntimeException("Couldn't retrieve chunk at " + pos.chunk());
        }

        LevelChunkSection section = getChunkSection(level, chunk, pos);

        if (LevelChunkSection.isEmpty(section)) {
            section = EMPTY_SECTION;
        }

        this.reset(pos);

        this.copyBlockData(section);
        this.copyLightData(level);
        this.copyBiomeData(chunk);
        this.copyBlockEntities(chunk, pos);
    }

    private void reset(SectionPos pos) {
        this.pos = pos;
        this.blockEntities.clear();
        this.renderAttachments.clear();

        this.blockStateData = null;
        this.blockStatePalette = null;

        this.biomeData = null;

        Arrays.fill(this.lightDataArrays, null);
    }

    private void copyBlockData(LevelChunkSection section) {
        PalettedContainerExtended<BlockState> container = PalettedContainerExtended.cast(section.getStates());

        this.blockStateData = copyBlockData(container);
        this.blockStatePalette = copyPalette(container);
    }

    private void copyLightData(Level level) {
        for (LightLayer type : LIGHT_TYPES) {
            this.lightDataArrays[type.ordinal()] = level.getLightEngine()
                    .getLayerListener(type)
                    .getDataLayerData(pos);
        }
    }

    private void copyBiomeData(ChunkAccess chunk) {
        this.biomeData = chunk.getBiomes();
    }

    public int getLightLevel(LightLayer type, int x, int y, int z) {
        DataLayer array = this.lightDataArrays[type.ordinal()];

        if (array != null) {
            return array.get(x, y, z);
        }

        return 0;
    }

    private void copyBlockEntities(LevelChunk chunk, SectionPos chunkCoord) {
        BoundingBox box = new BoundingBox(chunkCoord.minBlockX(), chunkCoord.minBlockY(), chunkCoord.minBlockZ(),
                chunkCoord.maxBlockX(), chunkCoord.maxBlockY(), chunkCoord.maxBlockZ());

        // Copy the block entities from the chunk into our cloned section
        for (Map.Entry<BlockPos, BlockEntity> entry : chunk.getBlockEntities().entrySet()) {
            BlockPos pos = entry.getKey();
            BlockEntity entity = entry.getValue();

            if (box.isInside(pos)) {
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

    public Biome getBiomeForNoiseGen(int x, int y, int z) {
        return this.biomeData.getNoiseBiome(x, y, z);
    }

    public BlockEntity getBlockEntity(int x, int y, int z) {
        return this.blockEntities.get(packLocal(x, y, z));
    }

    public Object getBlockEntityRenderAttachment(int x, int y, int z) {
        return this.renderAttachments.get(packLocal(x, y, z));
    }

    public BitStorage getBlockData() {
        return this.blockStateData;
    }

    public ClonedPalette<BlockState> getBlockPalette() {
        return this.blockStatePalette;
    }

    public SectionPos getPosition() {
        return this.pos;
    }

    private static ClonedPalette<BlockState> copyPalette(PalettedContainerExtended<BlockState> container) {
        Palette<BlockState> palette = container.getPalette();

        if (palette instanceof GlobalPalette) {
            return new ClonedPaletteFallback<>(Block.BLOCK_STATE_REGISTRY);
        }

        BlockState[] array = new BlockState[1 << container.getBits()];

        for (int i = 0; i < array.length; i++) {
            array[i] = palette.valueFor(i);

            if (array[i] == null) {
                break;
            }
        }

        return new ClonedPalleteArray<>(array, container.getDefaultValue());
    }

    private static BitStorage copyBlockData(PalettedContainerExtended<BlockState> container) {
        BitStorage array = container.getDataArray();
        long[] storage = array.getRaw();

        return new BitStorage(container.getBits(), array.getSize(), storage.clone());
    }

    private static LevelChunkSection getChunkSection(Level level, ChunkAccess chunk, SectionPos pos) {
        LevelChunkSection section = null;

        if (!level.isOutsideBuildHeight(SectionPos.sectionToBlockCoord(pos.getY()))) {
            section = chunk.getSections()[level.getSectionIndexFromSectionY(pos.getY())];
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
