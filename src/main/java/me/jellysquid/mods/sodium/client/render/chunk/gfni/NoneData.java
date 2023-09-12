package me.jellysquid.mods.sodium.client.render.chunk.gfni;

import net.minecraft.util.math.ChunkSectionPos;

/**
 * None translucent data exists to signal to GFNI that if a section previously
 * had triggering data, it no longer does and should be removed.
 */
public class NoneData extends TranslucentData {
    public NoneData(ChunkSectionPos sectionPos) {
        super(sectionPos);
    }

    @Override
    public SortType getSortType() {
        return SortType.NONE;
    }
}
