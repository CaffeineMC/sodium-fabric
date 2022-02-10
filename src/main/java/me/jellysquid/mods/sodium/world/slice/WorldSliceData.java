package me.jellysquid.mods.sodium.world.slice;

import me.jellysquid.mods.sodium.world.slice.cloned.ClonedChunkSection;
import me.jellysquid.mods.sodium.world.slice.cloned.ClonedChunkSectionCache;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

public class WorldSliceData {
    private final SectionPos origin;
    private final ClonedChunkSection[] sections;
    private final BoundingBox volume;

    public WorldSliceData(SectionPos origin, ClonedChunkSection[] sections, BoundingBox volume) {
        this.origin = origin;
        this.sections = sections;
        this.volume = volume;
    }

    public static WorldSliceData prepare(Level world, SectionPos origin, ClonedChunkSectionCache sectionCache) {
        LevelChunk chunk = world.getChunk(origin.getX(), origin.getZ());
        LevelChunkSection section = chunk.getSections()[world.getSectionIndexFromSectionY(origin.getY())];

        // If the chunk section is absent or empty, simply terminate now. There will never be anything in this chunk
        // section to render, so we need to signal that a chunk render task shouldn't created. This saves a considerable
        // amount of time in queueing instant build tasks and greatly accelerates how quickly the world can be loaded.
        if (section == null || section.hasOnlyAir()) {
            return null;
        }

        BoundingBox volume = new BoundingBox(
                origin.minBlockX() - WorldSlice.NEIGHBOR_BLOCK_RADIUS,
                origin.minBlockY() - WorldSlice.NEIGHBOR_BLOCK_RADIUS,
                origin.minBlockZ() - WorldSlice.NEIGHBOR_BLOCK_RADIUS,
                origin.maxBlockX() + WorldSlice.NEIGHBOR_BLOCK_RADIUS,
                origin.maxBlockY() + WorldSlice.NEIGHBOR_BLOCK_RADIUS,
                origin.maxBlockZ() + WorldSlice.NEIGHBOR_BLOCK_RADIUS);

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

    public SectionPos getOrigin() {
        return this.origin;
    }

    public BoundingBox getVolume() {
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
