package me.jellysquid.mods.sodium.world.slice.cloned;



import it.unimi.dsi.fastutil.shorts.Short2ObjectMap;
import it.unimi.dsi.fastutil.shorts.Short2ObjectMaps;
import it.unimi.dsi.fastutil.shorts.Short2ObjectOpenHashMap;
import me.jellysquid.mods.sodium.interop.vanilla.mixin.PalettedContainerAccessor;
import me.jellysquid.mods.sodium.world.slice.cloned.palette.ClonedPalette;
import me.jellysquid.mods.sodium.world.slice.cloned.palette.ClonedPaletteFallback;
import me.jellysquid.mods.sodium.world.slice.cloned.palette.ClonedPalleteArray;
import net.fabricmc.fabric.api.rendering.data.v1.RenderAttachmentBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.data.BuiltinRegistries;
import net.minecraft.util.BitStorage;
import net.minecraft.util.SimpleBitStorage;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.DataLayer;
import net.minecraft.world.level.chunk.GlobalPalette;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.Palette;
import net.minecraft.world.level.chunk.PalettedContainer;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class ClonedChunkSection {
    private static final LightLayer[] LIGHT_TYPES = LightLayer.values();

    @Deprecated
    private static final LevelChunkSection EMPTY_SECTION = new LevelChunkSection(0, BuiltinRegistries.BIOME);

    private static final int PACKED_BLOCK_BITS = 4;

    private final AtomicInteger referenceCount = new AtomicInteger(0);
    private final ClonedChunkSectionCache backingCache;

    private final Short2ObjectMap<BlockEntity> blockEntities;
    private final Short2ObjectMap<Object> renderAttachments;

    private final DataLayer[] lightDataArrays;

    private SectionPos pos;

    private SimpleBitStorage blockStateData;
    private ClonedPalette<BlockState> blockStatePalette;

    private PalettedContainer<Biome> biomeData;
    private boolean isEmpty = true;

    ClonedChunkSection(ClonedChunkSectionCache backingCache) {
        this.backingCache = backingCache;
        this.blockEntities = new Short2ObjectOpenHashMap<>();
        this.renderAttachments = new Short2ObjectOpenHashMap<>();
        this.lightDataArrays = new DataLayer[LIGHT_TYPES.length];
    }

    public void init(Level world, SectionPos pos) {
        LevelChunk chunk = world.getChunk(pos.getX(), pos.getZ());

        if (chunk == null) {
            throw new RuntimeException("Couldn't retrieve chunk at " + pos.chunk());
        }

        LevelChunkSection section = getChunkSection(world, chunk, pos);

        if (section == null) {
            section = EMPTY_SECTION;
        }

        this.reset(pos);

        this.copyBlockData(section);
        this.copyLightData(world);
        this.copyBiomeData(section);
        this.copyBlockEntities(chunk, pos);

        this.isEmpty = section.hasOnlyAir();
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
        PalettedContainer.Data<BlockState> container = PalettedContainerAccessor.getData(section.getStates());

        this.blockStateData = copyBlockData(container);
        this.blockStatePalette = copyPalette(container);
    }

    private void copyLightData(Level world) {
        for (LightLayer type : LIGHT_TYPES) {
            this.lightDataArrays[type.ordinal()] = world.getLightEngine()
                    .getLayerListener(type)
                    .getDataLayerData(this.pos);
        }
    }

    private void copyBiomeData(LevelChunkSection section) {
        this.biomeData = section.getBiomes();
    }

    public int getLightLevel(LightLayer type, int x, int y, int z) {
        DataLayer array = this.lightDataArrays[type.ordinal()];

        if (array != null) {
            return array.get(x, y, z);
        }

        return 0;
    }

    private void copyBlockEntities(LevelChunk chunk, SectionPos sectionCoord) {
        BoundingBox box = new BoundingBox(sectionCoord.minBlockX(), sectionCoord.minBlockY(), sectionCoord.minBlockZ(),
                sectionCoord.maxBlockX(), sectionCoord.maxBlockY(), sectionCoord.maxBlockZ());

        // Copy the block entities from the chunk into our cloned section
        for (Map.Entry<BlockPos, BlockEntity> entry : chunk.getBlockEntities().entrySet()) {
            BlockPos pos = entry.getKey();
            BlockEntity entity = entry.getValue();

            if (box.isInside(pos)) {
                this.blockEntities.put(SectionPos.sectionRelativePos(pos), entity);
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

    public Biome getBiome(int x, int y, int z) {
        return this.biomeData.get(x, y, z);
    }

    public BlockEntity getBlockEntity(int x, int y, int z) {
        return this.blockEntities.get(packLocal(x, y, z));
    }

    public Object getBlockEntityRenderAttachment(int x, int y, int z) {
        return this.renderAttachments.get(packLocal(x, y, z));
    }

    public SimpleBitStorage getBlockData() {
        return this.blockStateData;
    }

    public ClonedPalette<BlockState> getBlockPalette() {
        return this.blockStatePalette;
    }

    public SectionPos getPosition() {
        return this.pos;
    }

    private static ClonedPalette<BlockState> copyPalette(PalettedContainer.Data<BlockState> container) {
        Palette<BlockState> palette = container.palette();

        if (palette instanceof GlobalPalette) {
            return new ClonedPaletteFallback<>(Block.BLOCK_STATE_REGISTRY);
        }

        BlockState[] array = new BlockState[container.palette().getSize()];

        for (int i = 0; i < array.length; i++) {
            array[i] = palette.valueFor(i);
        }

        return new ClonedPalleteArray<>(array);
    }

    private static SimpleBitStorage copyBlockData(PalettedContainer.Data<BlockState> container) {
        var storage = container.storage();
        var data = storage.getRaw();
        var bits = container.configuration().bits();

        if (bits == 0) {
            // TODO: avoid this allocation
            return new SimpleBitStorage(1, storage.getSize());
        }

        return new SimpleBitStorage(bits, storage.getSize(), data.clone());
    }

    private static LevelChunkSection getChunkSection(Level world, ChunkAccess chunk, SectionPos pos) {
        LevelChunkSection section = null;

        if (!world.isOutsideBuildHeight(SectionPos.sectionToBlockCoord(pos.getY()))) {
            section = chunk.getSections()[world.getSectionIndexFromSectionY(pos.getY())];
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
     * @param localBlockX The local block x-coordinate
     * @param localBlockY The local block y-coordinate
     * @param localBlockZ The local block z-coordinate
     * @return A packed index which can be used to access entities or blocks within a section
     */
    private static short packLocal(int localBlockX, int localBlockY, int localBlockZ) {
        return (short) (localBlockX << PACKED_BLOCK_BITS << PACKED_BLOCK_BITS | localBlockZ << PACKED_BLOCK_BITS | localBlockY);
    }

    public BlockState getBlockState(int blockIdx) {
        return this.blockStatePalette.get(this.blockStateData.get(blockIdx));
    }

    public boolean isEmpty() {
        return this.isEmpty;
    }
}
