package me.jellysquid.mods.sodium.client.render.chunk.translucent_sorting.data;

import me.jellysquid.mods.sodium.client.gl.util.VertexRange;
import me.jellysquid.mods.sodium.client.render.chunk.translucent_sorting.AccGroupResult;
import me.jellysquid.mods.sodium.client.render.chunk.translucent_sorting.SortType;
import me.jellysquid.mods.sodium.client.util.NativeBuffer;
import net.minecraft.util.math.ChunkSectionPos;

public abstract class DynamicData extends MixedDirectionData {
    private AccGroupResult accGroupResult;

    DynamicData(ChunkSectionPos sectionPos, NativeBuffer buffer, VertexRange range, AccGroupResult accGroupResult) {
        super(sectionPos, buffer, range);
        this.accGroupResult = accGroupResult;
    }

    @Override
    public SortType getSortType() {
        return SortType.DYNAMIC_ALL;
    }

    public AccGroupResult getAccGroupResult() {
        return this.accGroupResult;
    }

    public void clearAccGroupData() {
        this.accGroupResult = null;
    }
}
