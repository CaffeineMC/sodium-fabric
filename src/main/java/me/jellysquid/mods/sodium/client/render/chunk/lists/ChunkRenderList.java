package me.jellysquid.mods.sodium.client.render.chunk.lists;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import me.jellysquid.mods.sodium.client.render.chunk.RenderSection;
import me.jellysquid.mods.sodium.client.render.chunk.region.RenderRegion;
import me.jellysquid.mods.sodium.client.util.ReversibleArrayIterator;
import me.jellysquid.mods.sodium.client.util.SectionIterator;

import java.util.function.Consumer;

public class ChunkRenderList {
    public final ObjectArrayList<RegionRenderLists> batches;

    public ChunkRenderList(ObjectArrayList<RegionRenderLists> batches) {
        this.batches = batches;
    }

    public static ChunkRenderList empty() {
        return new ChunkRenderList(ObjectArrayList.of());
    }

    public ReversibleArrayIterator<RegionRenderLists> sortedRegions(boolean reverse) {
        return new ReversibleArrayIterator<>(this.batches, reverse);
    }

    public void forEachSectionWithSprites(Consumer<RenderSection> consumer) {
        for (var batch : this.batches) {
            forEachSection(batch.region, batch.getSectionsWithSprites(false), consumer);
        }
    }

    public void forEachSectionWithEntities(Consumer<RenderSection> consumer) {
        for (var batch : this.batches) {
            forEachSection(batch.region, batch.getSectionsWithBlockEntities(false), consumer);
        }
    }

    private static void forEachSection(RenderRegion region, SectionIterator iterator, Consumer<RenderSection> consumer) {
        RenderSection[] sections = region.getChunks();

        while (iterator.hasNext()) {
            consumer.accept(sections[iterator.next()]);
        }
    }

    public int size() {
        int size = 0;

        for (var regionList : this.batches) {
            size += regionList.getSectionsWithGeometryCount();
        }

        return size;
    }
}
