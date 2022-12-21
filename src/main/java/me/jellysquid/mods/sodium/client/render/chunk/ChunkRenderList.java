package me.jellysquid.mods.sodium.client.render.chunk;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectSortedMap;
import it.unimi.dsi.fastutil.objects.*;
import me.jellysquid.mods.sodium.client.render.chunk.region.RenderRegion;
import me.jellysquid.mods.sodium.client.render.chunk.region.RenderRegionManager;

import java.util.List;

public class ChunkRenderList {
    private final Long2ObjectLinkedOpenHashMap<IntArrayList> entries = new Long2ObjectLinkedOpenHashMap<>();
    private final ObjectArrayFIFOQueue<IntArrayList> pool = new ObjectArrayFIFOQueue<>();

    private final RenderRegionManager regionManager;

    public ChunkRenderList(RenderRegionManager regionManager) {

        this.regionManager = regionManager;
    }

    public RenderRegion getRegion(long id) {
        return this.regionManager.getRegion(id);
    }

    public Long2ObjectSortedMap.FastSortedEntrySet<IntArrayList> sorted() {
        return this.entries.long2ObjectEntrySet();
    }

    public void clear() {
        this.pool.clear();

        for (var list : this.entries.values()) {
            list.clear();

            this.pool.enqueue(list);
        }

        this.entries.clear();
    }

    public void add(RenderSection section, int visibleFaces) {
        var regionId = section.getRegionId();
        var localId = section.getLocalId();

        IntArrayList sections = this.entries.get(regionId);

        if (sections == null) {
            this.entries.put(regionId, sections = this.createList());
        }

        sections.add(pack(localId, visibleFaces));
    }

    private IntArrayList createList() {
        if (this.pool.isEmpty()) {
            return new IntArrayList(RenderRegion.REGION_SIZE);
        }

        return this.pool.dequeue();
    }

    public int getCount() {
        return this.entries.values()
                .stream()
                .mapToInt(List::size)
                .sum();
    }

    public static int pack(int localId, int faces) {
        return ((localId & 0xFFFF) << 16) | (faces & 0xFF);
    }

    public static int unpackLocalId(int packed) {
        return ((packed >> 16) & 0xFFFF);
    }

    public static int unpackVisibleFaces(int packed) {
        return packed & 0xFF;
    }
}
