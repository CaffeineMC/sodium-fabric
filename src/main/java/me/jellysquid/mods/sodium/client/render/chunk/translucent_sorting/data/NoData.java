package me.jellysquid.mods.sodium.client.render.chunk.translucent_sorting.data;

import me.jellysquid.mods.sodium.client.render.chunk.translucent_sorting.SortType;
import net.minecraft.util.math.ChunkSectionPos;

/**
 * This class means there is no translucent data and is used to signal that the
 * section should be removed from triggering data structures.
 * 
 * If translucent sorting is disabled, not even this class is used, but null is
 * passed instead.
 */
public class NoData extends TranslucentData {
    public NoData(ChunkSectionPos sectionPos) {
        super(sectionPos);
    }

    @Override
    public SortType getSortType() {
        return SortType.NONE;
    }
}
