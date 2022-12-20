package me.jellysquid.mods.sodium.client.world.cloned;

import it.unimi.dsi.fastutil.longs.Long2ReferenceMap;
import it.unimi.dsi.fastutil.longs.Long2ReferenceOpenHashMap;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.world.World;

import java.util.concurrent.ConcurrentLinkedQueue;

public class ClonedChunkSectionCache {
    private final World world;

    private final Long2ReferenceMap<ClonedChunkSection> byPosition = new Long2ReferenceOpenHashMap<>();

    public ClonedChunkSectionCache(World world) {
        this.world = world;
    }

    public ClonedChunkSection acquire(int x, int y, int z) {
        long key = ChunkSectionPos.asLong(x, y, z);
        ClonedChunkSection section = this.byPosition.get(key);

        if (section == null) {
            section = this.createSection(x, y, z);
        }

        return section;
    }

    private ClonedChunkSection createSection(int x, int y, int z) {
        ChunkSectionPos pos = ChunkSectionPos.from(x, y, z);
        ClonedChunkSection section = new ClonedChunkSection(this.world, pos);

        this.byPosition.put(pos.asLong(), section);

        return section;
    }
}
