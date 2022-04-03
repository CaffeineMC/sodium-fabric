package net.caffeinemc.sodium.render.chunk.draw;

import it.unimi.dsi.fastutil.objects.*;
import net.caffeinemc.sodium.render.chunk.RenderSection;
import net.caffeinemc.sodium.render.chunk.region.RenderRegion;
import net.caffeinemc.sodium.util.IteratorUtils;

import java.util.Iterator;

public class ChunkRenderList {
    private final ReferenceArrayList<RenderSection> sections;
    private final ReferenceArrayList<Bucket> regions;

    public ChunkRenderList(ReferenceArrayList<RenderSection> sections) {
        RenderRegion cachedRegion = null;
        ReferenceArrayList<RenderSection> cachedList = null;

        var regions = new Reference2ReferenceLinkedOpenHashMap<RenderRegion, ReferenceArrayList<RenderSection>>();

        for (RenderSection section : sections) {
            var region = section.getRegion();

            if (cachedRegion != region) {
                cachedRegion = region;
                cachedList = regions.get(region);

                if (cachedList == null) {
                    regions.put(region, cachedList = new ReferenceArrayList<>(RenderRegion.REGION_SIZE / 4));
                }
            }

            cachedList.add(section);
        }

        this.regions = flatten(regions);
        this.sections = sections;
    }

    private static ReferenceArrayList<Bucket> flatten(Reference2ReferenceSortedMap<RenderRegion, ReferenceArrayList<RenderSection>> regions) {
        var array = new ReferenceArrayList<Bucket>(regions.size());

        for (var entry : Reference2ReferenceSortedMaps.fastIterable(regions)) {
            array.add(new Bucket(entry.getKey(), entry.getValue()));
        }

        return array;
    }

    public void add(RenderSection render) {
        this.sections.add(render);
    }

    public int sectionCount() {
        return this.sections.size();
    }

    public int regionCount() {
        return this.regions.size();
    }

    public Iterator<Bucket> sorted(boolean reverse) {
        return IteratorUtils.reversibleIterator(this.regions, reverse);
    }

    public Iterable<Bucket> unsorted() {
        return this.regions;
    }

    public record Bucket(RenderRegion region,
                         ReferenceArrayList<RenderSection> sections) {
        public int size() {
            return this.sections.size();
        }

        public Iterator<RenderSection> sorted(boolean reverse) {
            return IteratorUtils.reversibleIterator(this.sections, reverse);
        }
    }
}
