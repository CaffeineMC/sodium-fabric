package me.jellysquid.mods.sodium.client.render.chunk;

import it.unimi.dsi.fastutil.ints.Int2ReferenceOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ReferenceOpenHashMap;
import net.minecraft.util.math.ChunkSectionPos;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Iterator;

public class IndexedMap<T extends IndexedMap.IdHolder> {
    private final Long2ReferenceOpenHashMap<T> byPosition = new Long2ReferenceOpenHashMap<>();
    private T[] byId;

    private final IntPool idPool = new IntPool();

    private final Factory<T> factory;

    @SuppressWarnings("unchecked")
    public IndexedMap(Class<T> type, Factory<T> factory) {
        this.factory = factory;
        this.byId = (T[]) Array.newInstance(type, 512);
    }

    public T create(int x, int y, int z) {
        long pos = key(x, y, z);

        if (this.byPosition.containsKey(pos)) {
            throw new RuntimeException("Entry already exists at {x=%s,y=%s,z=%s}".formatted(x, y, z));
        }

        int id = this.idPool.create();

        T entry = this.factory.create(x, y, z, id);

        this.byPosition.put(pos, entry);

        if (id >= this.byId.length) {
            this.byId = Arrays.copyOf(this.byId, Math.max(this.byId.length * 2, id + 1024));
        }

        this.byId[id] = entry;

        return entry;
    }

    public Iterator<T> iterator() {
        return this.byPosition.values().iterator();
    }

    public T get(int x, int y, int z) {
        return this.byPosition.get(key(x, y, z));
    }

    public T getById(int id) {
        return this.byId[id];
    }

    private static long key(int x, int y, int z) {
        return ChunkSectionPos.asLong(x, y, z);
    }

    public void clear() {
        this.byPosition.clear();
        Arrays.fill(this.byId, null);

        this.idPool.reset();
    }

    public T getOrCreate(int x, int y, int z) {
        var entry = this.get(x, y, z);

        if (entry == null) {
            entry = this.create(x, y, z);
        }

        return entry;
    }

    public int getMaxId() {
        return this.idPool.size();
    }

    public T remove(int x, int y, int z) {
        var entry = this.byPosition.remove(key(x, y, z));

        if (entry == null) {
            throw new RuntimeException("");
        }

        return entry;
    }

    public int size() {
        return this.byPosition.size();
    }

    public interface Factory<T> {
        T create(int x, int y, int z, int id);
    }

    public interface IdHolder {
        int id();
    }
}
