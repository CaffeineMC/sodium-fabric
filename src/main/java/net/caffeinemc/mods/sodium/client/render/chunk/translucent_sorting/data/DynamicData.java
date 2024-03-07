package net.caffeinemc.mods.sodium.client.render.chunk.translucent_sorting.data;

import org.joml.Vector3dc;

import net.caffeinemc.mods.sodium.client.gl.util.VertexRange;
import net.caffeinemc.mods.sodium.client.render.chunk.translucent_sorting.SortType;
import net.caffeinemc.mods.sodium.client.render.chunk.translucent_sorting.trigger.GeometryPlanes;
import net.caffeinemc.mods.sodium.client.util.NativeBuffer;
import net.minecraft.core.SectionPos;

public abstract class DynamicData extends MixedDirectionData {
    private GeometryPlanes geometryPlanes;
    private final Vector3dc initialCameraPos;

    DynamicData(SectionPos sectionPos, NativeBuffer buffer, VertexRange range, GeometryPlanes geometryPlanes, Vector3dc initialCameraPos) {
        super(sectionPos, buffer, range);
        this.geometryPlanes = geometryPlanes;
        this.initialCameraPos = initialCameraPos;
    }

    @Override
    public SortType getSortType() {
        return SortType.DYNAMIC;
    }

    @Override
    public boolean retainAfterUpload() {
        return true;
    }

    public GeometryPlanes getGeometryPlanes() {
        return this.geometryPlanes;
    }

    public void clearGeometryPlanes() {
        this.geometryPlanes = null;
    }

    public Vector3dc getInitialCameraPos() {
        return this.initialCameraPos;
    }
}
