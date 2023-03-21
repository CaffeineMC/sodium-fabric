package me.jellysquid.mods.sodium.client.render.chunk.data;

import me.jellysquid.mods.sodium.client.gl.arena.GlBufferSegment;
import me.jellysquid.mods.sodium.client.model.quad.properties.ModelQuadFacing;
import me.jellysquid.mods.sodium.client.render.chunk.region.RenderRegion;

import java.util.Arrays;

public class SectionRenderDataStorage {
    private final GlBufferSegment[] allocations = new GlBufferSegment[RenderRegion.REGION_SIZE];

    private final long pArray;

    public SectionRenderDataStorage() {
        this.pArray = SectionRenderDataUnsafe.allocateHeap(RenderRegion.REGION_SIZE);
    }

    public void setData(int localSectionIndex, SectionRenderData state) {
        this.allocations[localSectionIndex] = state.buffer;

        var pData = this.getDataPointer(localSectionIndex);

        SectionRenderDataUnsafe.setBaseVertex(pData, state.buffer.getOffset());

        SectionRenderDataUnsafe.setBatchSize(pData, ModelQuadFacing.POS_X, state.modelPosX);
        SectionRenderDataUnsafe.setBatchSize(pData, ModelQuadFacing.POS_Y, state.modelPosY);
        SectionRenderDataUnsafe.setBatchSize(pData, ModelQuadFacing.POS_Z, state.modelPosZ);

        SectionRenderDataUnsafe.setBatchSize(pData, ModelQuadFacing.NEG_X, state.modelNegX);
        SectionRenderDataUnsafe.setBatchSize(pData, ModelQuadFacing.NEG_Y, state.modelNegY);
        SectionRenderDataUnsafe.setBatchSize(pData, ModelQuadFacing.NEG_Z, state.modelNegZ);

        SectionRenderDataUnsafe.setBatchSize(pData, ModelQuadFacing.UNASSIGNED, state.modelUnassigned);
    }

    public long getDataPointer(int section) {
        return SectionRenderDataUnsafe.heapPointer(this.pArray, section);
    }

    public void delete() {
        for (var allocation : this.allocations) {
            if (allocation != null) {
                allocation.delete();
            }
        }

        SectionRenderDataUnsafe.freeHeap(this.pArray);

        Arrays.fill(this.allocations, null);
    }

    public void replaceData(int localSectionIndex, SectionRenderData state) {
        this.deleteData(localSectionIndex);

        if (state != null) {
            this.setData(localSectionIndex, state);
        }
    }

    public void deleteData(int localSectionIndex) {
        var allocation = this.allocations[localSectionIndex];

        if (allocation != null) {
            allocation.delete();
        }

        SectionRenderDataUnsafe.clear(this.getDataPointer(localSectionIndex));

        this.allocations[localSectionIndex] = null;
    }

    public void refreshPointers() {
        for (int sectionIndex = 0; sectionIndex < RenderRegion.REGION_SIZE; sectionIndex++) {
            var allocation = this.allocations[sectionIndex];

            if (allocation != null) {
                SectionRenderDataUnsafe.setBaseVertex(this.getDataPointer(sectionIndex), allocation.getOffset());
            }
        }
    }
}
