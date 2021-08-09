package me.jellysquid.mods.sodium.client.world.cloned;

import it.unimi.dsi.fastutil.longs.Long2ReferenceMap;
import it.unimi.dsi.fastutil.longs.Long2ReferenceOpenHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.Level;

public class ClonedChunkSectionCache {
    private final Level world;

    private final ConcurrentLinkedQueue<ClonedChunkSection> inactivePool = new ConcurrentLinkedQueue<>();
    private final Long2ReferenceMap<ClonedChunkSection> byPosition = new Long2ReferenceOpenHashMap<>();

    public ClonedChunkSectionCache(Level world) {
        this.world = world;
    }

    public ClonedChunkSection acquire(int x, int y, int z) {
        long key = SectionPos.asLong(x, y, z);
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

        SectionPos pos = SectionPos.of(x, y, z);
        section.init(this.world, pos);

        this.byPosition.put(pos.asLong(), section);

        return section;
    }

    public void invalidate(int x, int y, int z) {
        this.byPosition.remove(SectionPos.asLong(x, y, z));
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
