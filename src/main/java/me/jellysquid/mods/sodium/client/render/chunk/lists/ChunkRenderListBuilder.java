package me.jellysquid.mods.sodium.client.render.chunk.lists;

import it.unimi.dsi.fastutil.longs.Long2ReferenceLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import me.jellysquid.mods.sodium.client.render.chunk.RenderSection;
import me.jellysquid.mods.sodium.client.render.chunk.region.RenderRegion;

public class ChunkRenderListBuilder {

    private final Long2ReferenceLinkedOpenHashMap<ObjectArrayList<RenderSection>> entries = new Long2ReferenceLinkedOpenHashMap<>();

    public void add(RenderSection render) {
        ObjectArrayList<RenderSection> sections = this.entries.get(render.getRegionId());
        if (sections == null) {
            this.entries.put(render.getRegionId(), sections = new ObjectArrayList<>(RenderRegion.REGION_SIZE));
        }
        sections.add(render);
    }

    public ChunkRenderList build() {
        var batches = new ObjectArrayList<ChunkRenderList.RegionBatch>(this.entries.size());
        for (var entry : this.entries.long2ReferenceEntrySet()) {
            batches.add(new ChunkRenderList.RegionBatch(entry.getLongKey(), entry.getValue()));
        }
        return new ChunkRenderList(batches);
    }
}
