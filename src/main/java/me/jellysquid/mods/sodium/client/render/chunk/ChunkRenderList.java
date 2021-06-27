package me.jellysquid.mods.sodium.client.render.chunk;

import com.google.common.collect.Lists;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.Reference2ObjectMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectMaps;

import java.util.List;
import java.util.Map;

public class ChunkRenderList {
    private final List<Entry> entries = new ObjectArrayList<>();

    public Iterable<Entry> iterable(boolean reverse) {
        return reverse ? Lists.reverse(this.entries) : this.entries;
    }

    public void clear() {
        this.entries.clear();
    }

    public void addChunks(Reference2ObjectMap<RenderRegion, List<RenderChunk>> visibleChunks) {
        for (Map.Entry<RenderRegion, List<RenderChunk>> entry : Reference2ObjectMaps.fastIterable(visibleChunks)) {
            this.entries.add(new Entry(entry.getKey(), entry.getValue()));
        }
    }

    public static class Entry {
        private final RenderRegion region;
        private final List<RenderChunk> chunks;

        public Entry(RenderRegion region, List<RenderChunk> chunks) {
            this.region = region;
            this.chunks = chunks;
        }

        public RenderRegion getRegion() {
            return this.region;
        }

        public Iterable<RenderChunk> iterable(boolean reverse) {
            return reverse ? Lists.reverse(this.chunks) : this.chunks;
        }
    }
}
