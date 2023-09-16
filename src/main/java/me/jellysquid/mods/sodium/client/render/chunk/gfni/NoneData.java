package me.jellysquid.mods.sodium.client.render.chunk.gfni;

import net.minecraft.util.math.ChunkSectionPos;

/**
 * None translucent data exists to signal to GFNI that if a section previously
 * had triggering data, it no longer does and should be removed.
 * 
 * With this sort type the section's translucent quads can be rendered in any
 * order. However, they do need to be rendered with some index buffer, so that
 * vertices are assembled into quads. Since the sort order doesn't matter, all
 * sections with this sort type can share the same data in the index buffer.
 * 
 * TODO: make shared NoneData in the SectionRenderDataStorage or sth
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
