package me.jellysquid.mods.sodium.client.render.chunk.translucent_sorting.data;

import me.jellysquid.mods.sodium.client.gl.util.VertexRange;
import me.jellysquid.mods.sodium.client.render.chunk.translucent_sorting.SortType;
import me.jellysquid.mods.sodium.client.render.chunk.translucent_sorting.trigger.GeometryPlanes;
import me.jellysquid.mods.sodium.client.util.NativeBuffer;
import net.minecraft.util.math.ChunkSectionPos;

public abstract class DynamicData extends MixedDirectionData {
    private GeometryPlanes geometryPlanes;

    DynamicData(ChunkSectionPos sectionPos, NativeBuffer buffer, VertexRange range, GeometryPlanes geometryPlanes) {
        super(sectionPos, buffer, range);
        this.geometryPlanes = geometryPlanes;
    }

    @Override
    public SortType getSortType() {
        return SortType.DYNAMIC;
    }

    public GeometryPlanes getGeometryPlanes() {
        return this.geometryPlanes;
    }

    public void clearGeometryPlanes() {
        this.geometryPlanes = null;
    }
}
