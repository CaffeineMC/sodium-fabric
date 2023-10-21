package me.jellysquid.mods.sodium.client.render.chunk.lists;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import me.jellysquid.mods.sodium.client.render.chunk.RenderSection;
import me.jellysquid.mods.sodium.client.util.iterator.ReversibleObjectArrayIterator;
import me.jellysquid.mods.sodium.client.render.chunk.region.RenderRegion;
import net.minecraft.util.math.Vec3d;

import java.util.Comparator;

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
        private static final Comparator<ChunkRenderList> LIST_DISTANCE_COMPARATOR = Comparator.comparingDouble(ChunkRenderList::getDistanceFromCamera);
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

        public SortedRenderLists build(Vec3d cameraPos) {
            //noinspection ForLoopReplaceableByForEach
            for (int i = 0; i < this.lists.size(); i++) {
                ChunkRenderList renderList = this.lists.get(i);
                RenderRegion region = renderList.getRegion();
                double dx = cameraPos.x - region.getCenterX();
                double dy = cameraPos.y - region.getCenterY();
                double dz = cameraPos.z - region.getCenterZ();
                renderList.setDistanceFromCamera((dx * dx) + (dy * dy) + (dz * dz));
            }
            this.lists.sort(LIST_DISTANCE_COMPARATOR);
            return new SortedRenderLists(this.lists);
        }
    }
}
