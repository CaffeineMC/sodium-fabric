package me.jellysquid.mods.sodium.world.slice;

import me.jellysquid.mods.sodium.world.slice.cloned.ClonedChunkSection;
import me.jellysquid.mods.sodium.world.slice.cloned.ClonedChunkSectionCache;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.WorldChunk;

public class WorldSliceData {
    private final ChunkSectionPos origin;
    private final ClonedChunkSection[] sections;
    private final BlockBox volume;

    public WorldSliceData(ChunkSectionPos origin, ClonedChunkSection[] sections, BlockBox volume) {
        this.origin = origin;
        this.sections = sections;
        this.volume = volume;
    }

    public static WorldSliceData prepare(World world, ChunkSectionPos origin, ClonedChunkSectionCache sectionCache) {
        WorldChunk chunk = world.getChunk(origin.getX(), origin.getZ());
        ChunkSection section = chunk.getSectionArray()[world.sectionCoordToIndex(origin.getY())];

        // If the chunk section is absent or empty, simply terminate now. There will never be anything in this chunk
        // section to render, so we need to signal that a chunk render task shouldn't created. This saves a considerable
        // amount of time in queueing instant build tasks and greatly accelerates how quickly the world can be loaded.
        if (section == null || section.isEmpty()) {
            return null;
        }

        BlockBox volume = new BlockBox(
                origin.getMinX() - WorldSlice.NEIGHBOR_BLOCK_RADIUS,
                origin.getMinY() - WorldSlice.NEIGHBOR_BLOCK_RADIUS,
                origin.getMinZ() - WorldSlice.NEIGHBOR_BLOCK_RADIUS,
                origin.getMaxX() + WorldSlice.NEIGHBOR_BLOCK_RADIUS,
                origin.getMaxY() + WorldSlice.NEIGHBOR_BLOCK_RADIUS,
                origin.getMaxZ() + WorldSlice.NEIGHBOR_BLOCK_RADIUS);

        // The min/max bounds of the sections copied by this slice
        final int minSectionX = origin.getX() - WorldSlice.NEIGHBOR_SECTION_RADIUS;
        final int minSectionY = origin.getY() - WorldSlice.NEIGHBOR_SECTION_RADIUS;
        final int minSectionZ = origin.getZ() - WorldSlice.NEIGHBOR_SECTION_RADIUS;

        final int maxSectionX = origin.getX() + WorldSlice.NEIGHBOR_SECTION_RADIUS;
        final int maxSectionY = origin.getY() + WorldSlice.NEIGHBOR_SECTION_RADIUS;
        final int maxSectionZ = origin.getZ() + WorldSlice.NEIGHBOR_SECTION_RADIUS;

        ClonedChunkSection[] sections = new ClonedChunkSection[WorldSlice.SECTION_TABLE_ARRAY_SIZE];

        for (int sectionX = minSectionX; sectionX <= maxSectionX; sectionX++) {
            for (int sectionZ = minSectionZ; sectionZ <= maxSectionZ; sectionZ++) {
                for (int sectionY = minSectionY; sectionY <= maxSectionY; sectionY++) {
                    sections[WorldSlice.packSectionIndex(sectionX - minSectionX, sectionY - minSectionY, sectionZ - minSectionZ)] =
                            sectionCache.acquire(sectionX, sectionY, sectionZ);
                }
            }
        }

        return new WorldSliceData(origin, sections, volume);
    }

    public ClonedChunkSection[] getSections() {
        return this.sections;
    }

    public ChunkSectionPos getOrigin() {
        return this.origin;
    }

    public BlockBox getVolume() {
        return this.volume;
    }

    public void releaseResources() {
        for (ClonedChunkSection section : this.sections) {
            if (section != null) {
                section.getBackingCache()
                        .release(section);
            }
        }
    }
}
