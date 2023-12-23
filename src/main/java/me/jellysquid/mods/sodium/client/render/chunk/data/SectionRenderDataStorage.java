package me.jellysquid.mods.sodium.client.render.chunk.data;

import me.jellysquid.mods.sodium.client.gl.arena.GlBufferSegment;
import me.jellysquid.mods.sodium.client.gl.util.VertexRange;
import me.jellysquid.mods.sodium.client.model.quad.properties.ModelQuadFacing;
import me.jellysquid.mods.sodium.client.render.chunk.region.RenderRegion;

import java.util.Arrays;

/**
 * The section render data storage stores the gl buffer segments of uploaded
 * data on the gpu. There's one storage object per region. It stores information
 * about vertex and optionally index buffer data. The array of buffer segment is
 * indexed by the region-local section index. The data about the contents of
 * buffer segments is stored in a natively allocated piece of memory referenced
 * by {@code pMeshDataArray} and accessed through
 * {@link SectionRenderDataUnsafe}.
 * 
 * When the backing buffer (from the gl buffer arena) is resized, the storage
 * object is notified and then it updates the changed offsets of the buffer
 * segments. Since the index data's size and alignment directly corresponds to
 * that of the vertex data except for the vertex/index scaling of two thirds,
 * only an offset to the index data within the index data buffer arena is
 * stored.
 * 
 * Index and vertex data storage can be managed separately since they may be
 * updated independently of each other (in both directions).
 */
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

    public void removeData(int localSectionIndex) {
        this.removeVertexData(localSectionIndex, false);
        if (this.storesIndices) {
            this.removeIndexData(localSectionIndex);
        }
    }

    public void removeVertexData(int localSectionIndex) {
        this.removeVertexData(localSectionIndex, true);
    }

    private void removeVertexData(int localSectionIndex, boolean retainIndexData) {
        if (this.allocations[localSectionIndex] == null) {
            return;
        }

        this.allocations[localSectionIndex].delete();
        this.allocations[localSectionIndex] = null;

        var pMeshData = this.getDataPointer(localSectionIndex);
        var indexOffset = SectionRenderDataUnsafe.getIndexOffset(pMeshData);
        SectionRenderDataUnsafe.clear(pMeshData);
        SectionRenderDataUnsafe.setIndexOffset(pMeshData, indexOffset);
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
    }

    public void onIndexBufferResized() {
        if (this.storesIndices) {
            for (int sectionIndex = 0; sectionIndex < RenderRegion.REGION_SIZE; sectionIndex++) {
                this.updateIndexes(sectionIndex);
            }
        }
    }

    private void updateIndexes(int sectionIndex) {
        var data = this.getDataPointer(sectionIndex);
        var indexAllocation = this.allocations[sectionIndex + RenderRegion.REGION_SIZE];
        if (indexAllocation != null) {
            SectionRenderDataUnsafe.setIndexOffset(data, indexAllocation.getOffset());
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
