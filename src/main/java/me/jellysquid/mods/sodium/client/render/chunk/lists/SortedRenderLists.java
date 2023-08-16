package me.jellysquid.mods.sodium.client.render.chunk.lists;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import me.jellysquid.mods.sodium.client.render.chunk.RenderSection;
import me.jellysquid.mods.sodium.client.util.iterator.ReversibleObjectArrayIterator;
import me.jellysquid.mods.sodium.client.render.chunk.region.RenderRegion;

public class SortedRenderLists implements ChunkRenderListIterable {
    private static final SortedRenderLists EMPTY = new SortedRenderLists(ObjectArrayList.of());

    private final ObjectArrayList<ChunkRenderList> lists;

    SortedRenderLists(ObjectArrayList<ChunkRenderList> lists) {
        this.lists = lists;
    }

    @Override
    public ReversibleObjectArrayIterator<ChunkRenderList> iterator(boolean reverse) {
        return new ReversibleObjectArrayIterator<>(this.lists, reverse);
    }

    public static SortedRenderLists empty() {
        return EMPTY;
    }

    public static class Builder {
        private final ObjectArrayList<ChunkRenderList> lists = new ObjectArrayList<>();
        private final int frame;

        public Builder(int frame) {
            this.frame = frame;
        }

        public void add(RenderSection section) {
            RenderRegion region = section.getRegion();
            ChunkRenderList list = region.getRenderList();

            if (list.getLastVisibleFrame() != this.frame) {
                list.reset(this.frame);

                this.lists.add(list);
            }

            list.add(section);
        }

        public SortedRenderLists build() {
            return new SortedRenderLists(this.lists);
        }
    }
}
