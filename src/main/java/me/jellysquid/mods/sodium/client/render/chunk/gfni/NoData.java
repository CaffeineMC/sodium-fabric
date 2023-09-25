package me.jellysquid.mods.sodium.client.render.chunk.gfni;

import net.minecraft.util.math.ChunkSectionPos;

/**
 * This class means there is no translucent data and is used to signal to GFNI
 * to remove the section from the triggering structure.
 * 
 * If no translucent sorting is being performed, not even this class is used but
 * null is passed instead.
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
