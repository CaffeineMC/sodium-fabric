package me.jellysquid.mods.sodium.client.render.chunk.lists;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.longs.Long2ReferenceLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import me.jellysquid.mods.sodium.client.render.chunk.RenderSection;
import me.jellysquid.mods.sodium.client.render.chunk.region.RenderRegion;

public class ChunkRenderListBuilder {

    private final Long2ReferenceLinkedOpenHashMap<IntArrayList> entries = new Long2ReferenceLinkedOpenHashMap<>();

    public void add(RenderSection render) {
        IntArrayList sections = this.entries.get(render.getRegionId());

        if (sections == null) {
            this.entries.put(render.getRegionId(), sections = new IntArrayList(RenderRegion.REGION_SIZE));
        }

        sections.add(render.getChunkId());
    }

    public ChunkRenderList build() {
        var batches = new ObjectArrayList<ChunkRenderList.RegionBatch>(this.entries.size());

        for (var entry : this.entries.long2ReferenceEntrySet()) {
            batches.add(new ChunkRenderList.RegionBatch(entry.getLongKey(), entry.getValue()));
        }

        return new ChunkRenderList(batches);
    }

}
