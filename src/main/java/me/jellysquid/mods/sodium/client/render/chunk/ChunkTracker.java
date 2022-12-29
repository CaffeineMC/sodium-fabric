package me.jellysquid.mods.sodium.client.render.chunk;

import it.unimi.dsi.fastutil.longs.*;
import it.unimi.dsi.fastutil.objects.Reference2ReferenceOpenHashMap;
import net.minecraft.client.world.ClientChunkManager;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.ChunkPos;
import org.apache.commons.lang3.Validate;

import java.util.Map;

public class ChunkTracker {
    private final Long2IntOpenHashMap status = new Long2IntOpenHashMap();
    private final LongOpenHashSet ready = new LongOpenHashSet();

    private int centerChunkX, centerChunkZ;
    private int radius;

    private final Map<Area, Queues> areas = new Reference2ReferenceOpenHashMap<>();

    public ChunkTracker(int loadDistance) {
        this.status.defaultReturnValue(0);

        this.radius = getChunkMapRadius(loadDistance);
    }

    public static ChunkTracker from(ClientWorld world) {
        return from(world.getChunkManager());
    }
    public static ChunkTracker from(ClientChunkManager world) {
        return ((ChunkTracker.Accessor) world).getTracker();
    }

    public void updateCenter(int chunkX, int chunkZ) {
        this.centerChunkX = chunkX;
        this.centerChunkZ = chunkZ;

        this.checkChunks();
    }

    public void updateLoadDistance(int loadDistance) {
        this.radius = getChunkMapRadius(loadDistance);

        this.checkChunks();
    }

    private void checkChunks() {
        var queue = new LongArrayList();

        for (Long2IntMap.Entry entry : this.status.long2IntEntrySet()) {
            var pos = entry.getLongKey();

            var chunkX = ChunkPos.getPackedX(pos);
            var chunkZ = ChunkPos.getPackedZ(pos);

            if (!isInRadius(this.radius, this.centerChunkX, this.centerChunkZ, chunkX, chunkZ)) {
                queue.add(pos);
            }
        }

        this.unloadChunks(queue);
    }

    private void unloadChunks(LongArrayList queue) {
        var it = queue.longIterator();

        while (it.hasNext()) {
            long pos = it.nextLong();

            int x = ChunkPos.getPackedX(pos);
            int z = ChunkPos.getPackedZ(pos);

            this.remove(x, z);
        }
    }

    private static boolean isInRadius(int radius, int centerChunkX, int centerChunkZ, int chunkX, int chunkZ) {
        return Math.abs(chunkX - centerChunkX) <= radius &&
                Math.abs(chunkZ - centerChunkZ) <= radius;
    }

    private static int getChunkMapRadius(int loadDistance) {
        return Math.max(2, loadDistance) + 3;
    }

    private void updateMerged(int x, int z) {
        long key = ChunkPos.toLong(x, z);

        int flags = this.status.get(key);

        for (int ox = -1; ox <= 1; ox++) {
            for (int oz = -1; oz <= 1; oz++) {
                flags &= this.status.get(ChunkPos.toLong(ox + x, oz + z));
            }
        }

        if (flags == ChunkStatus.FLAG_ALL) {
            if (this.ready.add(key)) {
                this.notifyListeners(x, z, true);
            }
        } else {
            if (this.ready.remove(key)) {
                this.notifyListeners(x, z, false);
            }
        }
    }

    private void notifyListeners(int x, int z, boolean added) {
        long key = ChunkPos.toLong(x, z);

        for (var entry : this.areas.entrySet()) {
            var area = entry.getKey();
            var queues = entry.getValue();

            if (isInRadius(area.radius, area.x, area.z, x, z)) {
                if (added) {
                    queues.added.add(key);
                } else {
                    queues.removed.add(key);
                }
            }
        }
    }

    public void mark(int x, int z, int bits) {
        var key = ChunkPos.toLong(x, z);
        var prev = this.status.get(key);

        if ((prev & bits) == bits) {
            return;
        }

        this.status.put(key, prev | bits);

        for (int ox = -1; ox <= 1; ox++) {
            for (int oz = -1; oz <= 1; oz++) {
                this.updateMerged(ox + x, oz + z);
            }
        }
    }

    public void remove(int x, int z) {
        var key = ChunkPos.toLong(x, z);
        var prev = this.status.get(key);

        if (prev == 0) {
            return;
        }

        this.status.remove(key);

        for (int ox = -1; ox <= 1; ox++) {
            for (int oz = -1; oz <= 1; oz++) {
                this.updateMerged(ox + x, oz + z);
            }
        }
    }

    public record Area(int x, int z, int radius) {
    }

    public interface Accessor {
        ChunkTracker getTracker();
    }

    public void addWatchedArea(Area area) {
        Validate.isTrue(!this.areas.containsKey(area));

        var queues = new Queues();
        var it = this.ready.longIterator();

        while (it.hasNext()) {
            long pos = it.nextLong();

            int x = ChunkPos.getPackedX(pos);
            int z = ChunkPos.getPackedZ(pos);

            if (isInRadius(area.radius, area.x, area.z, x, z)) {
                queues.added.add(pos);
            }
        }

        this.areas.put(area, queues);
    }

    public void updateWatchedArea(Area before, Area after) {
        Validate.isTrue(this.areas.containsKey(before));
        Validate.isTrue(!this.areas.containsKey(after));

        var queues = this.areas.remove(before);
        var it = this.ready.longIterator();

        while (it.hasNext()) {
            long pos = it.nextLong();

            int x = ChunkPos.getPackedX(pos);
            int z = ChunkPos.getPackedZ(pos);

            boolean inBefore = isInRadius(before.radius, before.x, before.z, x, z);
            boolean inAfter = isInRadius(after.radius, after.x, after.z, x, z);

            if (inBefore && !inAfter) {
                queues.removed.add(pos);
            }

            if (!inBefore && inAfter) {
                queues.added.add(pos);
            }
        }

        this.areas.put(after, queues);
    }

    public void removeWatchedArea(Area area) {
        Validate.isTrue(this.areas.containsKey(area));

        this.areas.remove(area);
    }

    public Queues getEvents(Area area) {
        var queues = this.areas.get(area);

        if (queues == null) {
            return null;
        }

        this.areas.put(area, new Queues());

        return queues;
    }

    public static final class Queues {
        private final LongOpenHashSet added;
        private final LongOpenHashSet removed;

        public Queues() {
            this.added = new LongOpenHashSet();
            this.removed = new LongOpenHashSet();
        }

        public LongIterable added() {
            return this.added;
        }

        public LongIterable removed() {
            return this.removed;
        }
    }
}
