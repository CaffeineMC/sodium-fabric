package me.jellysquid.mods.sodium.client.world.cloned;

import it.unimi.dsi.fastutil.longs.Long2ReferenceLinkedOpenHashMap;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.WorldChunk;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.TimeUnit;

public class ClonedChunkSectionCache {
    private static final int MAX_CACHE_SIZE = 512; /* number of entries */
    private static final long MAX_CACHE_DURATION = TimeUnit.SECONDS.toNanos(5); /* number of nanoseconds */

    private final World world;

    private final Long2ReferenceLinkedOpenHashMap<ClonedChunkSection> positionToEntry = new Long2ReferenceLinkedOpenHashMap<>();

    private long time; // updated once per frame to be the elapsed time since application start

    public ClonedChunkSectionCache(World world) {
        this.world = world;
        this.time = getMonotonicTimeSource();
    }

    public void cleanup() {
        this.time = getMonotonicTimeSource();
        this.positionToEntry.values()
                .removeIf(entry -> this.time > (entry.getLastUsedTimestamp() + MAX_CACHE_DURATION));
    }

    @Nullable
    public ClonedChunkSection acquire(int x, int y, int z) {
        var pos = ChunkSectionPos.asLong(x, y, z);
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
        WorldChunk chunk = this.world.getChunk(x, z);

        if (chunk == null) {
            throw new RuntimeException("Chunk is not loaded at: " + ChunkSectionPos.asLong(x, y, z));
        }

        @Nullable ChunkSection section = null;

        if (!this.world.isOutOfHeightLimit(ChunkSectionPos.getBlockCoord(y))) {
            section = chunk.getSectionArray()[this.world.sectionCoordToIndex(y)];
        }

        return new ClonedChunkSection(this.world, chunk, section, ChunkSectionPos.from(x, y, z));
    }

    public void invalidate(int x, int y, int z) {
        this.positionToEntry.remove(ChunkSectionPos.asLong(x, y, z));
    }

    private static long getMonotonicTimeSource() {
        // Should be monotonic in JDK 17 on sane platforms...
        return System.nanoTime();
    }
}
