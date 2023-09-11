package me.jellysquid.mods.sodium.client.render.chunk.gfni;

import net.minecraft.util.math.ChunkSectionPos;

class NoneData extends TranslucentData {
    public NoneData(ChunkSectionPos sectionPos) {
        super(sectionPos);
    }

    @Override
    public SortType getSortType() {
        return SortType.NONE;
    }
}
