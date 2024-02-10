package net.caffeinemc.mods.sodium.client.world.cloned;

import it.unimi.dsi.fastutil.longs.Long2ReferenceLinkedOpenHashMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.TimeUnit;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;

/**
 * Provides a cache for {@link ClonedChunkSection} with an LRU eviction policy. This allows the renderer to avoid
 * repeatedly copying the same frequently-accessed chunks from the main thread.
 *
 * <p>Most notably, this helps during the initial world load, since chunks are being loaded in a concentric ring
 * outwards, and each section will depend on data from its neighbors. Given that each section will likely be copied
 * by each neighbor, this means a given chunk section during world load can be copied 25+ times without caching.</p>
 *
 * <p>The cache will evict entries which have not been accessed within a given duration, since keeping them around in
 * memory likely does not provide any benefit.</p>
 *
 * <p>When a chunk is modified, the cache must be notified so that it can evict the cached copy of its data. Otherwise,
 * future rebuild tasks for a section will see old data, and not render correctly.</p>
 */
public class ClonedChunkSectionCache {
    /**
     * The maximum number of entries in the cache before least-recently accessed entries start being evicted.
     */
    private static final int MAX_CACHE_SIZE = 512;

    /**
     * The maximum duration (in nanoseconds) which a cloned chunk section can remain idle before it is evicted
     * from the cache.
     */
    private static final long MAX_CACHE_DURATION = TimeUnit.SECONDS.toNanos(5);

    private final Level level;

    private final Long2ReferenceLinkedOpenHashMap<ClonedChunkSection> positionToEntry = new Long2ReferenceLinkedOpenHashMap<>();

    /**
     * The last time the cache was updated, used for evicting idle entries in the cache.
     */
    private long time;

    public ClonedChunkSectionCache(Level level) {
        this.level = level;
        this.time = getMonotonicTimeSource();
    }

    /**
     * Performs the clean-up of cached entries which are too old. This should be polled by the renderer occasionally
     * so that the cache doesn't keep a bunch of unnecessary copies in memory.
     */
    public void cleanup() {
        this.time = getMonotonicTimeSource();
        this.positionToEntry.values()
                .removeIf(entry -> this.time > (entry.getLastUsedTimestamp() + MAX_CACHE_DURATION));
    }

    /**
     * Acquires a chunk section from the cache. If the section isn't already cached, it will be copied from the world,
     * and then inserted into the cache.
     * @param x The x-coordinate of the section's position
     * @param y The y-coordinate of the section's position
     * @param z The z-coordinate of the section's position
     * @return An immutable copy of the chunk section in the world
     */
    @NotNull
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

    /**
     * Clones the chunk section from the world.
     * @param x The x-coordinate of the section's position
     * @param y The y-coordinate of the section's position
     * @param z The z-coordinate of the section's position
     * @return An immutable copy of the chunk section in the world
     */
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

        return new ClonedChunkSection(SectionPos.of(x, y, z), this.level, chunk, section);
    }

    /**
     * Invalidates any cached entry for the chunk section at the given coordinates. This should be called any time
     * the corresponding chunk section is modified in the world.
     * @param x The x-coordinate of the section's position
     * @param y The y-coordinate of the section's position
     * @param z The z-coordinate of the section's position
     */
    public void invalidate(int x, int y, int z) {
        this.positionToEntry.remove(SectionPos.asLong(x, y, z));
    }

    /**
     * A monotonic time source which can be used for elapsing time.
     * @return The current timestamp from the time source
     */
    private static long getMonotonicTimeSource() {
        // Should be monotonic in JDK 17 on sane platforms...
        return System.nanoTime();
    }
}
