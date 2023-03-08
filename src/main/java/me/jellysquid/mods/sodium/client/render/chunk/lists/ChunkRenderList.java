package me.jellysquid.mods.sodium.client.render.chunk.lists;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import me.jellysquid.mods.sodium.client.render.chunk.IndexedMap;
import me.jellysquid.mods.sodium.client.render.chunk.RenderSection;
import me.jellysquid.mods.sodium.client.render.chunk.region.RenderRegion;
import me.jellysquid.mods.sodium.client.util.ReversibleArrayIterator;

import java.util.function.Consumer;

public class ChunkRenderList {
    protected final ObjectArrayList<RegionRenderLists> batches;

    public ChunkRenderList(ObjectArrayList<RegionRenderLists> batches) {
        this.batches = batches;
    }

    public ReversibleArrayIterator<RegionRenderLists> sortedRegions(boolean reverse) {
        return new ReversibleArrayIterator<>(this.batches, reverse);
    }

    public void forEachSectionWithSprites(Consumer<RenderSection> consumer) {
        for (var regionList : this.batches) {
            var it = regionList.getSectionsWithSprites(false);
            it.forEach(consumer);
        }
    }

    public void forEachSectionWithEntities(Consumer<RenderSection> consumer) {
        for (var regionList : this.batches) {
            var it = regionList.getSectionsWithBlockEntities(false);
            it.forEach(consumer);
        }
    }

    public int size() {
        int size = 0;

        for (var regionList : this.batches) {
            size += regionList.size();
        }

        return size;
    }
}
