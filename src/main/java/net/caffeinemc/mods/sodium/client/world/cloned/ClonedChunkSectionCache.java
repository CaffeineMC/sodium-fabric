package net.caffeinemc.mods.sodium.client.world.cloned;

import it.unimi.dsi.fastutil.longs.Long2ReferenceLinkedOpenHashMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.TimeUnit;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;

public class ClonedChunkSectionCache {
    private static final int MAX_CACHE_SIZE = 512; /* number of entries */
    private static final long MAX_CACHE_DURATION = TimeUnit.SECONDS.toNanos(5); /* number of nanoseconds */

    private final Level level;

    private final Long2ReferenceLinkedOpenHashMap<ClonedChunkSection> positionToEntry = new Long2ReferenceLinkedOpenHashMap<>();

    private long time; // updated once per frame to be the elapsed time since application start

    public ClonedChunkSectionCache(Level level) {
        this.level = level;
        this.time = getMonotonicTimeSource();
    }

    public void cleanup() {
        this.time = getMonotonicTimeSource();
        this.positionToEntry.values()
                .removeIf(entry -> this.time > (entry.getLastUsedTimestamp() + MAX_CACHE_DURATION));
    }

    @Nullable
    public ClonedChunkSection acquire(int x, int y, int z) {
        var pos = SectionPos.asLong(x, y, z);
        var section = this.positionToEntry.getAndMoveToLast(pos);

        if (section == null) {
            section = this.clone(x, y, z);

            while (this.positionToEntry.size() >= MAX_CACHE_SIZE) {
                this.positionToEntry.removeFirst();
            }

            this.positionToEntry.putAndMoveToLast(pos, section);
        }

        section.setLastUsedTimestamp(this.time);

        return section;
    }

    @NotNull
    private ClonedChunkSection clone(int x, int y, int z) {
        LevelChunk chunk = this.level.getChunk(x, z);

        if (chunk == null) {
            throw new RuntimeException("Chunk is not loaded at: " + SectionPos.asLong(x, y, z));
        }

        @Nullable LevelChunkSection section = null;

        if (!this.level.isOutsideBuildHeight(SectionPos.sectionToBlockCoord(y))) {
            section = chunk.getSections()[this.level.getSectionIndexFromSectionY(y)];
        }

        return new ClonedChunkSection(this.level, chunk, section, SectionPos.of(x, y, z));
    }

    public void invalidate(int x, int y, int z) {
        this.positionToEntry.remove(SectionPos.asLong(x, y, z));
    }

    private static long getMonotonicTimeSource() {
        // Should be monotonic in JDK 17 on sane platforms...
        return System.nanoTime();
    }
}
