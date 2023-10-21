package me.jellysquid.mods.sodium.client.render.chunk.data;

import me.jellysquid.mods.sodium.client.gl.arena.GlBufferSegment;
import me.jellysquid.mods.sodium.client.gl.util.VertexRange;
import me.jellysquid.mods.sodium.client.model.quad.properties.ModelQuadFacing;
import me.jellysquid.mods.sodium.client.render.chunk.region.RenderRegion;

import java.util.Arrays;

public class SectionRenderDataStorage {
    private final GlBufferSegment[] allocations;

    private final long pMeshDataArray;
    private final boolean storesIndices;

    public SectionRenderDataStorage(boolean storesIndices) {
        var allocationCount = RenderRegion.REGION_SIZE * (storesIndices ? 2 : 1);
        this.allocations = new GlBufferSegment[allocationCount];
        this.pMeshDataArray = SectionRenderDataUnsafe.allocateHeap(RenderRegion.REGION_SIZE);
        this.storesIndices = storesIndices;
    }

    public void setVertexData(int localSectionIndex,
            GlBufferSegment allocation, VertexRange[] ranges) {
        if (this.allocations[localSectionIndex] != null) {
            this.allocations[localSectionIndex].delete();
            this.allocations[localSectionIndex] = null;
        }

        this.allocations[localSectionIndex] = allocation;

        var pMeshData = this.getDataPointer(localSectionIndex);

        int sliceMask = 0;
        int vertexOffset = allocation.getOffset();

        for (int facingIndex = 0; facingIndex < ModelQuadFacing.COUNT; facingIndex++) {
            VertexRange vertexRange = ranges[facingIndex];
            int vertexCount;

            if (vertexRange != null) {
                vertexCount = vertexRange.vertexCount();
            } else {
                vertexCount = 0;
            }

            SectionRenderDataUnsafe.setVertexOffset(pMeshData, facingIndex, vertexOffset);
            SectionRenderDataUnsafe.setElementCount(pMeshData, facingIndex, (vertexCount >> 2) * 6);

            if (vertexCount > 0) {
                sliceMask |= 1 << facingIndex;
            }

            vertexOffset += vertexCount;
        }

        SectionRenderDataUnsafe.setSliceMask(pMeshData, sliceMask);
    }

    public void setIndexData(int localSectionIndex, GlBufferSegment allocation) {
        if (!this.storesIndices) {
            throw new IllegalStateException("Cannot set index data when storesIndices is false");
        }

        var indexAllocationIndex = localSectionIndex + RenderRegion.REGION_SIZE;
        if (this.allocations[indexAllocationIndex] != null) {
            this.allocations[indexAllocationIndex].delete();
            this.allocations[indexAllocationIndex] = null;
        }

        this.allocations[indexAllocationIndex] = allocation;

        var pMeshData = this.getDataPointer(localSectionIndex);

        SectionRenderDataUnsafe.setIndexOffset(pMeshData, allocation.getOffset());
    }

    public void removeVertexData(int localSectionIndex) {
        if (this.allocations[localSectionIndex] == null) {
            return;
        }

        this.allocations[localSectionIndex].delete();
        this.allocations[localSectionIndex] = null;

        // also clear index allocation
        if (this.storesIndices) {
            removeIndexData(localSectionIndex);
        }

        SectionRenderDataUnsafe.clear(this.getDataPointer(localSectionIndex));
    }

    public void removeIndexData(int localSectionIndex) {
        if (!this.storesIndices) {
            throw new IllegalStateException("Cannot remove index data when storesIndices is false");
        }
        var indexAllocationIndex = localSectionIndex + RenderRegion.REGION_SIZE;
        if (this.allocations[indexAllocationIndex] != null) {
            this.allocations[indexAllocationIndex].delete();
            this.allocations[indexAllocationIndex] = null;
        }
    }

    public void onBufferResized() {
        for (int sectionIndex = 0; sectionIndex < RenderRegion.REGION_SIZE; sectionIndex++) {
            this.updateMeshes(sectionIndex);
        }
    }

    private void updateMeshes(int sectionIndex) {
        var allocation = this.allocations[sectionIndex];

        if (allocation == null) {
            return;
        }

        var offset = allocation.getOffset();
        var data = this.getDataPointer(sectionIndex);

        for (int facing = 0; facing < ModelQuadFacing.COUNT; facing++) {
            SectionRenderDataUnsafe.setVertexOffset(data, facing, offset);

            var count = SectionRenderDataUnsafe.getElementCount(data, facing);
            offset += (count / 6) * 4; // convert elements back into vertices
        }

        if (this.storesIndices) {
            var indexAllocation = this.allocations[sectionIndex + RenderRegion.REGION_SIZE];
            if (indexAllocation != null) {
                SectionRenderDataUnsafe.setIndexOffset(data, indexAllocation.getOffset());
            }
        }
    }

    public long getDataPointer(int sectionIndex) {
        return SectionRenderDataUnsafe.heapPointer(this.pMeshDataArray, sectionIndex);
    }

    public void delete() {
        for (var allocation : this.allocations) {
            if (allocation != null) {
                allocation.delete();
            }
        }

        Arrays.fill(this.allocations, null);

        SectionRenderDataUnsafe.freeHeap(this.pMeshDataArray);
    }
}
