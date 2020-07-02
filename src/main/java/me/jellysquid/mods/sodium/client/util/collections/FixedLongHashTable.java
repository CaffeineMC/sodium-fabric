package me.jellysquid.mods.sodium.client.util.collections;

import it.unimi.dsi.fastutil.Hash;
import it.unimi.dsi.fastutil.HashCommon;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;

import java.util.Arrays;
import java.util.Map;
import java.util.NoSuchElementException;

import static it.unimi.dsi.fastutil.HashCommon.arraySize;


/**
 * A Long->Object hash table of fixed size, allowing opportunistic readers in a multi-threaded scenario to retrieve
 * values without running into memory safety issues. If this map is mutated during an opportunistic read, the returned
 * result is invalid.
 *
 * This implementation is based on Long2ObjectOpenHashMap from the FastUtil project (http://fastutil.di.unimi.it/),
 * which is made available under the Apache License 2.0.
 *
 * @param <V> The value type of the hash table
 */
public class FixedLongHashTable<V> implements Hash {
    protected final long[] key;
    protected final V[] value;
    protected final int mask;
    protected final int capacity;

    protected boolean containsNullKey;
    protected int size;

    @SuppressWarnings("unchecked")
    public FixedLongHashTable(final int capacity, final float loadFactor) {
        if (loadFactor <= 0.0f || loadFactor > 1.0f) {
            throw new IllegalArgumentException("Load factor must be greater than 0 and smaller than or equal to 1");
        }

        if (capacity < 0) {
            throw new IllegalArgumentException("The expected number of elements must be non-negative");
        }

        this.capacity = arraySize(capacity, loadFactor);
        this.mask = this.capacity - 1;
        this.key = new long[this.capacity + 1];
        this.value = (V[]) new Object[this.capacity + 1];
    }

    private V removeEntry(final int pos) {
        final V prev = this.value[pos];
        this.value[pos] = null;
        this.size--;
        this.shiftKeys(pos);
        return prev;
    }

    private V removeNullEntry() {
        this.containsNullKey = false;
        final V prev = this.value[this.capacity];
        this.value[this.capacity] = null;
        this.size--;
        return prev;
    }

    private int find(final long k) {
        if (k == 0) {
            return this.containsNullKey ? this.capacity : -(this.capacity + 1);
        }

        final long[] key = this.key;
        long curr;
        int pos;

        // The starting point.
        if ((curr = key[pos = (int) HashCommon.mix(k) & this.mask]) == 0) {
            return -(pos + 1);
        }

        if (k == curr) {
            return pos;
        }

        // There's always an unused entry.
        while (true) {
            if ((curr = key[pos = pos + 1 & this.mask]) == 0) {
                return -(pos + 1);
            }

            if (k == curr) {
                return pos;
            }
        }
    }

    private void insert(final int pos, final long k, final V v) {
        if (pos == this.capacity) {
            this.containsNullKey = true;
        }

        this.key[pos] = k;
        this.value[pos] = v;

        if (this.size++ >= this.capacity) {
            throw new IllegalStateException("Exceeded capacity of map");
        }
    }

    public V put(final long k, final V v) {

        final int pos = this.find(k);
        if (pos < 0) {
            this.insert(-pos - 1, k, v);
            return null;
        }

        final V prev = this.value[pos];
        this.value[pos] = v;
        return prev;
    }

    protected final void shiftKeys(int pos) {
        final long[] key = this.key;

        // Shift entries with the same hash.
        int last, slot;
        long curr;

        while (true) {
            pos = (last = pos) + 1 & this.mask;

            while (true) {
                if ((curr = key[pos]) == 0) {
                    key[last] = 0;
                    this.value[last] = null;
                    return;
                }

                slot = (int) HashCommon.mix(curr) & this.mask;

                if (last <= pos ? last >= slot || slot > pos : last >= slot && slot > pos) {
                    break;
                }

                pos = pos + 1 & this.mask;
            }

            key[last] = curr;
            this.value[last] = this.value[pos];
        }
    }

    public V remove(final long k) {
        if (k == 0) {
            if (this.containsNullKey) {
                return this.removeNullEntry();
            }

            return null;
        }

        final long[] key = this.key;
        long curr;
        int pos;

        // The starting point.
        if ((curr = key[pos = (int) HashCommon.mix(k) & this.mask]) == 0) {
            return null;
        }

        if (k == curr) {
            return this.removeEntry(pos);
        }

        while (true) {
            if ((curr = key[pos = pos + 1 & this.mask]) == 0) {
                return null;
            }

            if (k == curr) {
                return this.removeEntry(pos);
            }
        }
    }

    public V get(final long k) {
        if (k == 0) {
            return this.containsNullKey ? this.value[this.capacity] : null;
        }

        final long[] key = this.key;
        long curr;
        int pos;

        // The starting point.
        if ((curr = key[pos = (int) HashCommon.mix(k) & this.mask]) == 0) {
            return null;
        }

        if (k == curr) {
            return this.value[pos];
        }

        // There's always an unused entry.
        while (true) {
            if ((curr = key[pos = pos + 1 & this.mask]) == 0) {
                return null;
            }
            if (k == curr) {
                return this.value[pos];
            }
        }
    }

    public void clear() {
        if (this.size == 0) {
            return;
        }

        this.size = 0;
        this.containsNullKey = false;

        Arrays.fill(this.key, 0);
        Arrays.fill(this.value, null);
    }

    public int size() {
        return this.size;
    }

    public boolean isEmpty() {
        return this.size == 0;
    }

    /**
     * The entry class for a hash map does not record key and value, but rather the
     * position in the hash table of the corresponding entry. This is necessary so
     * that calls to {@link Map.Entry#setValue(Object)} are reflected in
     * the map
     */
    private final class MapEntry implements Long2ObjectMap.Entry<V>, Map.Entry<Long, V> {
        private int index;

        @Override
        public long getLongKey() {
            return FixedLongHashTable.this.key[this.index];
        }

        @Override
        public V getValue() {
            return FixedLongHashTable.this.value[this.index];
        }

        @Override
        public V setValue(V value) {
            throw new UnsupportedOperationException();
        }

        @Override
        public String toString() {
            return FixedLongHashTable.this.key[this.index] + "=>" + FixedLongHashTable.this.value[this.index];
        }
    }

    private class FastEntryIterator implements ObjectIterator<Long2ObjectMap.Entry<V>> {
        private final FixedLongHashTable<V>.MapEntry entry = new FixedLongHashTable<V>.MapEntry();
        private int pos = FixedLongHashTable.this.capacity;

        private int c = FixedLongHashTable.this.size;
        private boolean mustReturnNullKey = FixedLongHashTable.this.containsNullKey;

        @Override
        public FixedLongHashTable<V>.MapEntry next() {
            this.entry.index = this.nextEntry();
            return this.entry;
        }

        public boolean hasNext() {
            return this.c != 0;
        }

        public int nextEntry() {
            if (!this.hasNext()) {
                throw new NoSuchElementException();
            }

            this.c--;

            if (this.mustReturnNullKey) {
                this.mustReturnNullKey = false;
                return FixedLongHashTable.this.capacity;
            }

            final long[] key = FixedLongHashTable.this.key;

            while (true) {
                if ((key[--this.pos]) != (0)) {
                    return this.pos;
                }
            }
        }
    }

    public ObjectIterator<Long2ObjectMap.Entry<V>> iterator() {
        return new FastEntryIterator();
    }
}
