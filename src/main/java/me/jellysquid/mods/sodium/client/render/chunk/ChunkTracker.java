package me.jellysquid.mods.sodium.client.render.chunk;

import it.unimi.dsi.fastutil.longs.*;
import net.minecraft.util.math.ChunkPos;

import java.util.stream.LongStream;

public class ChunkTracker {
    private final Long2ReferenceMap<ChunkStatus> chunks = new Long2ReferenceOpenHashMap<>();
    private final Long2IntOpenHashMap neighborCount = new Long2IntOpenHashMap();

    public ChunkTracker() {
        this.chunks.defaultReturnValue(ChunkStatus.NOT_LOADED);
        this.neighborCount.defaultReturnValue(0);
    }

    public boolean isRenderable(int x, int z) {
        var key = ChunkPos.toLong(x, z);
        return this.chunks.get(key) == ChunkStatus.READY && this.neighborCount.get(key) >= 9;
    }

    public boolean addChunk(int x, int z) {
        var key = ChunkPos.toLong(x, z);

        if (this.chunks.containsKey(key)) {
            return false;
        }

        this.chunks.put(key, ChunkStatus.AWAITING_LIGHT);

        for (int xd = -1; xd <= 1; xd++) {
            for (int zd = -1; zd <= 1; zd++) {
                this.neighborCount.addTo(ChunkPos.toLong(x + xd, z + zd), 1);
            }
        }

        return true;
    }

    public void onLightAdded(int x, int z) {
        var key = ChunkPos.toLong(x, z);

        if (!this.chunks.containsKey(key)) {
            throw new IllegalStateException("Tried to mark light status for chunk [%s, %s] but it wasn't loaded".formatted(x, z));
        }

        this.chunks.put(key, ChunkStatus.READY);
    }

    public boolean removeChunk(int x, int z) {
        long pos = ChunkPos.toLong(x, z);

        if (!this.chunks.containsKey(pos)) {
            return false;
        }

        this.chunks.remove(pos);

        for (int xd = -1; xd <= 1; xd++) {
            for (int zd = -1; zd <= 1; zd++) {
                long key = ChunkPos.toLong(x + xd, z + zd);

                if (this.neighborCount.addTo(key, -1) == 1) {
                    this.neighborCount.remove(key);
                }
            }
        }

        return true;
    }

    public LongStream getChunksWithAtLeastStatus(ChunkStatus status) {
        return this.chunks
                .long2ReferenceEntrySet()
                .stream()
                .filter(entry -> entry.getValue().ordinal() >= status.ordinal())
                .mapToLong(Long2ReferenceMap.Entry::getLongKey);
    }
}
