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
    private final SortType reason;

    private NoData(ChunkSectionPos sectionPos, SortType reason) {
        super(sectionPos);
        this.reason = reason;
    }

    @Override
    public SortType getSortType() {
        return reason;
    }

    public static NoData forEmptySection(ChunkSectionPos sectionPos) {
        return new NoData(sectionPos, SortType.EMPTY_SECTION);
    }

    public static NoData forNoTranslucent(ChunkSectionPos sectionPos) {
        return new NoData(sectionPos, SortType.NO_TRANSLUCENT);
    }
}
