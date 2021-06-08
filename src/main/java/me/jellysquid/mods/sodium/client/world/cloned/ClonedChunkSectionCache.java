package me.jellysquid.mods.sodium.client.world.cloned;

import it.unimi.dsi.fastutil.longs.Long2ReferenceMap;
import it.unimi.dsi.fastutil.longs.Long2ReferenceOpenHashMap;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.world.World;

import java.util.concurrent.ConcurrentLinkedQueue;

public class ClonedChunkSectionCache {
    private final World world;

    private final ConcurrentLinkedQueue<ClonedChunkSection> inactivePool = new ConcurrentLinkedQueue<>();
    private final Long2ReferenceMap<ClonedChunkSection> byPosition = new Long2ReferenceOpenHashMap<>();

    public ClonedChunkSectionCache(World world) {
        this.world = world;
    }

    public ClonedChunkSection acquire(int x, int y, int z) {
        long key = ChunkSectionPos.asLong(x, y, z);
        ClonedChunkSection section = this.byPosition.get(key);

        if (section != null) {
            this.inactivePool.remove(section);
        } else {
            section = this.createSection(x, y, z);
        }

        section.acquireReference();

        return section;
    }

    private ClonedChunkSection createSection(int x, int y, int z) {
        ClonedChunkSection section;

        if (!this.inactivePool.isEmpty()) {
            section = this.inactivePool.remove();

            this.byPosition.remove(section.getPosition().asLong());
        } else {
            section = this.allocate();
        }

        ChunkSectionPos pos = ChunkSectionPos.from(x, y, z);
        section.init(this.world, pos);

        this.byPosition.put(pos.asLong(), section);

        return section;
    }

    public void invalidate(int x, int y, int z) {
        this.byPosition.remove(ChunkSectionPos.asLong(x, y, z));
    }

    public void release(ClonedChunkSection section) {
        if (section.releaseReference()) {
            this.tryReclaim(section);
        }
    }

    private ClonedChunkSection allocate() {
        return new ClonedChunkSection(this);
    }

    private void tryReclaim(ClonedChunkSection section) {
        this.inactivePool.add(section);
    }
}
