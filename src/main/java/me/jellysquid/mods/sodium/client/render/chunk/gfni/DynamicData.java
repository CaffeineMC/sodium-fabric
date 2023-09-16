package me.jellysquid.mods.sodium.client.render.chunk.gfni;

import org.joml.Vector3f;
import org.joml.Vector3fc;

import it.unimi.dsi.fastutil.ints.Int2ReferenceLinkedOpenHashMap;
import me.jellysquid.mods.sodium.client.gl.util.VertexRange;
import me.jellysquid.mods.sodium.client.util.NativeBuffer;
import me.jellysquid.mods.sodium.client.util.sorting.VertexSorters;
import net.minecraft.util.math.ChunkSectionPos;

public class DynamicData extends MixedDirectionData {
    public final Vector3f[] centers;
    private AccumulationGroup[] axisAlignedDistances;
    private Int2ReferenceLinkedOpenHashMap<AccumulationGroup> unalignedDistances;

    public DynamicData(ChunkSectionPos sectionPos,
            NativeBuffer buffer, VertexRange range, Vector3f[] centers,
            AccumulationGroup[] axisAlignedDistances,
            Int2ReferenceLinkedOpenHashMap<AccumulationGroup> unalignedDistances) {
        super(sectionPos, buffer, range);
        this.centers = centers;
        this.axisAlignedDistances = axisAlignedDistances;
        this.unalignedDistances = unalignedDistances;
    }

    @Override
    public SortType getSortType() {
        return SortType.DYNAMIC_ALL;
    }

    AccumulationGroup getGroupForNormal(NormalList normalList) {
        int groupBuilderKey = normalList.getGroupBuilderKey();
        if (groupBuilderKey < 0xFF) {
            if (this.axisAlignedDistances == null) {
                return null;
            }
            return this.axisAlignedDistances[groupBuilderKey];
        } else {
            if (this.unalignedDistances == null) {
                return null;
            }
            return this.unalignedDistances.get(groupBuilderKey);
        }
    }

    public AccumulationGroup[] getAxisAlignedDistances() {
        return this.axisAlignedDistances;
    }

    public Int2ReferenceLinkedOpenHashMap<AccumulationGroup> getUnalignedDistances() {
        return this.unalignedDistances;
    }

    public void clearTriggerData() {
        this.axisAlignedDistances = null;
        this.unalignedDistances = null;
    }

    @Override
    public void sort(Vector3fc cameraPos) {
        var intBuffer = this.buffer.getDirectBuffer().asIntBuffer();
        TranslucentData.writeVertexIndexes(intBuffer,
                VertexSorters.sortByDistance(new Vector3f(cameraPos)).sort(this.centers));
    }
}
