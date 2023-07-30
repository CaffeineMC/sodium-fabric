package me.jellysquid.mods.sodium.client.world.cloned;

import it.unimi.dsi.fastutil.longs.Long2ReferenceLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.ReferenceLinkedOpenHashSet;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.WorldChunk;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.concurrent.TimeUnit;

public class ClonedChunkSectionCache {
    private static final int MAX_CACHE_SIZE = 512; /* number of entries */
    private static final long MAX_CACHE_DURATION = TimeUnit.SECONDS.toNanos(5); /* number of seconds */

    private final World world;

    private final Long2ReferenceLinkedOpenHashMap<ClonedChunkSection> positionToEntry = new Long2ReferenceLinkedOpenHashMap<>();
    private final ReferenceLinkedOpenHashSet<ClonedChunkSection> inactiveEntries = new ReferenceLinkedOpenHashSet<>();

    private final ReferenceLinkedOpenHashSet<ClonedChunkSection> invalidatedEntries = new ReferenceLinkedOpenHashSet<>();

    private final ArrayDeque<ClonedChunkSection> freeEntries = new ArrayDeque<>();

    private long currentTime = System.nanoTime();

    public ClonedChunkSectionCache(World world) {
        this.world = world;
    }

    public void update() {
        this.currentTime = System.nanoTime();

        this.invalidateExcessiveEntries();
        this.invalidateOutdatedEntries();

        this.tryReclaimInvalidated();

        this.markInactiveEntries();
    }

    private void markInactiveEntries() {
        this.inactiveEntries.clear();

        for (var entry : this.positionToEntry.values()) {
            if (entry.getReferenceCount() == 0) {
                this.inactiveEntries.add(entry);
            }
        }
    }

    private void tryReclaimInvalidated() {
        var it = this.invalidatedEntries.iterator();

        while (it.hasNext()) {
            var entry = it.next();

            if (entry.getReferenceCount() == 0) {
                entry.clear();

                if (this.freeEntries.size() < 256) {
                    this.freeEntries.add(entry);
                }

                it.remove();
            }
        }
    }

    private void invalidateOutdatedEntries() {
        var it = this.positionToEntry.values()
                .iterator();

        while (it.hasNext()) {
            var entry = it.next();

            if (this.currentTime > (entry.getLastUsedTimestamp() + MAX_CACHE_DURATION)) {
                it.remove();

                this.invalidatedEntries.add(entry);
            }
        }
    }

    private void invalidateExcessiveEntries() {
        // Ensure the cache does not get too large, by removing the oldest entries within it
        while (this.positionToEntry.size() > MAX_CACHE_SIZE) {
            var entry = this.positionToEntry.removeFirst();

            this.invalidatedEntries.add(entry);
        }
    }

    @Nullable
    public ClonedChunkSection acquire(int x, int y, int z) {
        var cloned = this.positionToEntry.getAndMoveToLast(ChunkSectionPos.asLong(x, y, z));

        if (cloned != null) {
            this.inactiveEntries.remove(cloned);
        } else {
            cloned = this.clone(x, y, z);

            // There was nothing to clone, because that section is empty
            if (cloned == null) {
                return null;
            }
        }

        cloned.acquireReference();
        cloned.setLastUsedTimestamp(this.currentTime);

        return cloned;
    }

    @Nullable
    private ClonedChunkSection clone(int x, int y, int z) {
        WorldChunk chunk = this.world.getChunk(x, z);

        if (chunk == null) {
            return null;
        }

        ChunkSection section = getChunkSection(this.world, chunk, y);

        if (section == null) {
            return null;
        }

        ChunkSectionPos sectionCoord = ChunkSectionPos.from(x, y, z);

        ClonedChunkSection clonedSection = this.allocate();

        clonedSection.setLastUsedTimestamp(this.currentTime);
        clonedSection.copy(this.world, chunk, section, sectionCoord);

        this.positionToEntry.putAndMoveToLast(sectionCoord.asLong(), clonedSection);

        return clonedSection;
    }

    @NotNull
    private ClonedChunkSection allocate() {
        if (!this.freeEntries.isEmpty()) {
            return this.takeFreeEntry();
        }

        if (!this.inactiveEntries.isEmpty()) {
            return this.takeInactiveEntry();
        }

        return new ClonedChunkSection();
    }

    @NotNull
    private ClonedChunkSection takeFreeEntry() {
        return this.freeEntries.removeFirst();
    }

    @NotNull
    private ClonedChunkSection takeInactiveEntry() {
        var section = this.inactiveEntries.removeFirst();
        var position = section.getPosition();

        this.positionToEntry.remove(position.asLong());

        section.clear();

        return section;
    }

    public void invalidate(int x, int y, int z) {
        // When invalidating, the section is immediately removed from the cache, which prevents
        // it from being taken by future build tasks.
        var section = this.positionToEntry.remove(ChunkSectionPos.asLong(x, y, z));

        if (section != null) {
            this.invalidate(section);
        }
    }

    private void invalidate(ClonedChunkSection section) {
        this.invalidatedEntries.add(section);
    }

    public String getDebugString() {
        int total = this.positionToEntry.size() + this.invalidatedEntries.size() + this.freeEntries.size();

        return String.format("T=%03d (IA=%03d | IV=%03d | F=%03d)",
                total, this.inactiveEntries.size(), this.invalidatedEntries.size(), this.freeEntries.size());
    }

    @Nullable
    private static ChunkSection getChunkSection(World world, Chunk chunk, int y) {
        ChunkSection section = null;

        if (!world.isOutOfHeightLimit(ChunkSectionPos.getBlockCoord(y))) {
            section = chunk.getSectionArray()[world.sectionCoordToIndex(y)];
        }

        return section;
    }
}
