package me.jellysquid.mods.sodium.client.render.chunk.lists;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import me.jellysquid.mods.sodium.client.util.ReversibleArrayIterator;
import me.jellysquid.mods.sodium.client.util.ReversibleIntArrayIterator;

public class ChunkRenderList {
    private final ObjectArrayList<RegionBatch> batches;

    public ChunkRenderList(ObjectArrayList<RegionBatch> batches) {
        this.batches = batches;
    }

    public ReversibleArrayIterator<RegionBatch> batches(boolean reverse) {
        return new ReversibleArrayIterator<>(this.batches, reverse);
    }

    public int getCount() {
        int total = 0;

        for (var batch : this.batches) {
            total += batch.size();
        }

        return total;
    }

    public static class RegionBatch {
        private final long regionId;
        private final IntArrayList sections;

        public RegionBatch(long regionId, IntArrayList sections) {
            this.regionId = regionId;
            this.sections = sections;
        }

        public long getRegionId() {
            return this.regionId;
        }

        public ReversibleIntArrayIterator ordered(boolean reverse) {
            return new ReversibleIntArrayIterator(this.sections, reverse);
        }

        public int size() {
            return this.sections.size();
        }
    }
}
