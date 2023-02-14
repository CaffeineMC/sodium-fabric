package me.jellysquid.mods.sodium.client.render.chunk.lists;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import me.jellysquid.mods.sodium.client.render.chunk.RenderSection;
import me.jellysquid.mods.sodium.client.util.ReversibleArrayIterator;

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
        private final ObjectArrayList<RenderSection> sections;

        public RegionBatch(long regionId, ObjectArrayList<RenderSection> sections) {
            this.regionId = regionId;
            this.sections = sections;
        }

        public long getRegionId() {
            return this.regionId;
        }

        public ReversibleArrayIterator<RenderSection> ordered(boolean reverse) {
            return new ReversibleArrayIterator<>(this.sections, reverse);
        }

        public int size() {
            return this.sections.size();
        }
    }
}
