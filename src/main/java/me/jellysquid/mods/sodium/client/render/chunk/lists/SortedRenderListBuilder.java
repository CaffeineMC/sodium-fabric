package me.jellysquid.mods.sodium.client.render.chunk.lists;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.Reference2ReferenceLinkedOpenHashMap;
import me.jellysquid.mods.sodium.client.render.chunk.RenderSection;
import me.jellysquid.mods.sodium.client.render.chunk.region.RenderRegion;

import java.util.ArrayDeque;

public class SortedRenderListBuilder {
    private final Reference2ReferenceLinkedOpenHashMap<RenderRegion, ChunkRenderList> lists =
            new Reference2ReferenceLinkedOpenHashMap<>();
    private final ArrayDeque<ChunkRenderList> cache = new ArrayDeque<>();

    public void add(RenderSection render, int faces) {
        var region = render.getRegion();
        var list = this.lists.get(region);

        if (list == null) {
            this.lists.put(region, list = this.createRenderList(region));
        }

        list.add(render, faces);
    }

    private ChunkRenderList createRenderList(RenderRegion region) {
        ChunkRenderList renderList = this.cache.poll();

        if (renderList == null) {
            renderList = new ChunkRenderList();
        }

        renderList.init(region);

        return renderList;
    }

    public void reset() {
        this.cache.clear();
        this.cache.addAll(this.lists.values());

        this.lists.clear();
    }

    public SortedRenderLists build() {
        var batches = new ObjectArrayList<ChunkRenderList>(this.lists.size());
        batches.addAll(this.lists.values());

        return new SortedRenderLists(batches);
    }
}
