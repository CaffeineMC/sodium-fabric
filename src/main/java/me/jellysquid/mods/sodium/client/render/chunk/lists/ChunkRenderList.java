package me.jellysquid.mods.sodium.client.render.chunk.lists;

import me.jellysquid.mods.sodium.client.render.chunk.RenderSection;
import me.jellysquid.mods.sodium.client.render.chunk.region.RenderRegion;
import me.jellysquid.mods.sodium.client.util.ReversibleArrayIterator;
import me.jellysquid.mods.sodium.client.util.CLocalSectionListIterator;
import me.jellysquid.mods.sodium.core.types.CRegionDrawBatch;
import me.jellysquid.mods.sodium.core.types.CVec;

import java.util.function.Consumer;

public class ChunkRenderList {
    private final int count;
    private final long addr;

    public ChunkRenderList(CVec vec) {
        this.count = vec.len();
        this.addr = vec.data();
    }

    public ReversibleArrayIterator<CRegionDrawBatch> sortedRegions(boolean reverse) {
        CRegionDrawBatch[] batches = new CRegionDrawBatch[this.count];

        for (int i = 0; i < this.count; i++) {
            batches[i] = this.batch(i);
        }

        return new ReversibleArrayIterator<>(batches, reverse);
    }

//    public void forEachSectionWithSprites(Consumer<RenderSection> consumer) {
//        for (var batch : this.batches) {
//            forEachSection(batch.region, batch.getSectionsWithSprites(false), consumer);
//        }
//    }
//
//    public void forEachSectionWithEntities(Consumer<RenderSection> consumer) {
//        for (var batch : this.batches) {
//            forEachSection(batch.region, batch.getSectionsWithBlockEntities(false), consumer);
//        }
//    }

    private static void forEachSection(RenderRegion region, CLocalSectionListIterator iterator, Consumer<RenderSection> consumer) {
        RenderSection[] sections = region.getChunks();

        while (iterator.hasNext()) {
            consumer.accept(sections[iterator.next()]);
        }
    }

    public int size() {
        int size = 0;

        for (int i = 0; i < this.count; i++) {
            var regionBatch = this.batch(i);
            var sectionList = regionBatch.sectionList();

            size += sectionList.size();
        }

        return size;
    }

    private CRegionDrawBatch batch(int index) {
        return CRegionDrawBatch.fromHeap(this.addr + ((long) index * CRegionDrawBatch.SIZEOF));
    }
}
