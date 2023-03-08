package me.jellysquid.mods.sodium.client.render.chunk.lists;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import me.jellysquid.mods.sodium.client.render.chunk.IndexedMap;
import me.jellysquid.mods.sodium.client.render.chunk.region.RenderRegion;

public class ChunkRenderListBuilder {
    private final IndexedMap<RenderRegion> regions;
    private final ObjectArrayList<RegionRenderLists> sorted = new ObjectArrayList<>();

    private final Cache cache;

    public ChunkRenderListBuilder(IndexedMap<RenderRegion> regions, Cache cache) {
        this.regions = regions;
        this.batches = new RegionRenderLists[regions.getMaxId()];
        this.cache = cache;
    }

    private final RegionRenderLists[] batches;

    public void add(int flags, int region, int section) {
        RegionRenderLists list = this.batches[region];

        if (list == null) {
            list = this.createLists(region);
        }

        list.add(section, flags);
    }

    private RegionRenderLists createLists(int region) {
        RegionRenderLists list = this.cache.createLists();
        list.setRegion(this.regions.getById(region));

        this.batches[region] = list;
        this.sorted.add(list);

        return list;
    }

    public ChunkRenderList build() {
        return new ChunkRenderList(this.sorted);
    }

    public static class Cache {
        private final ObjectArrayList<RegionRenderLists> pool = new ObjectArrayList<>();

        public void add(ChunkRenderList list) {
            for (var batch : list.batches) {
                batch.reset();
            }

            this.pool.addAll(list.batches);
        }

        public RegionRenderLists createLists() {
            if (!this.pool.isEmpty()) {
                return this.pool.pop();
            }

            return new RegionRenderLists();
        }
    }
}
