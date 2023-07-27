package me.jellysquid.mods.sodium.client.render.chunk.data;

import me.jellysquid.mods.sodium.client.gl.arena.GlBufferSegment;
import me.jellysquid.mods.sodium.client.gl.util.VertexRange;
import me.jellysquid.mods.sodium.client.model.quad.properties.ModelQuadFacing;
import me.jellysquid.mods.sodium.client.render.chunk.region.RenderRegion;

import java.util.Arrays;

public class SectionRenderDataStorage {
    private final GlBufferSegment[] allocations = new GlBufferSegment[RenderRegion.REGION_SIZE];

    private final long pArray;

    public SectionRenderDataStorage() {
        this.pArray = SectionRenderDataUnsafe.allocateHeap(RenderRegion.REGION_SIZE);
    }

    public void addMesh(int localSectionIndex,
                        GlBufferSegment allocation, VertexRange[] ranges) {
        if (this.allocations[localSectionIndex] != null) {
            throw new IllegalStateException("Mesh already added");
        }

        this.allocations[localSectionIndex] = allocation;

        var data = this.getDataPointer(localSectionIndex);

        int offset = allocation.getOffset();
        int mask = 0;

        for (int facing = 0; facing < ModelQuadFacing.COUNT; facing++) {
            VertexRange range = ranges[facing];
            int count;

            if (range != null) {
                count = range.vertexCount();
            } else {
                count = 0;
            }

            SectionRenderDataUnsafe.setVertexOffset(data, facing, offset);
            SectionRenderDataUnsafe.setElementCount(data, facing, (count >> 2) * 6);

            if (count > 0) {
                mask |= 1 << facing;
            }

            offset += count;
        }

        SectionRenderDataUnsafe.setSliceMask(data, mask);
    }

    public void removeMesh(int localSectionIndex) {
        if (this.allocations[localSectionIndex] == null) {
            return;
        }

        this.allocations[localSectionIndex].delete();
        this.allocations[localSectionIndex] = null;

        SectionRenderDataUnsafe.clear(this.getDataPointer(localSectionIndex));
    }

    public void delete() {
        for (var allocation : this.allocations) {
            if (allocation != null) {
                allocation.delete();
            }
        }

        Arrays.fill(this.allocations, null);

        SectionRenderDataUnsafe.freeHeap(this.pArray);
    }

    public void refresh() {
        for (int sectionIndex = 0; sectionIndex < RenderRegion.REGION_SIZE; sectionIndex++) {
            var allocation = this.allocations[sectionIndex];

            if (allocation == null) {
                continue;
            }

            var offset = allocation.getOffset();
            var data = this.getDataPointer(sectionIndex);

            for (int facing = 0; facing < ModelQuadFacing.COUNT; facing++) {
                SectionRenderDataUnsafe.setVertexOffset(data, facing, offset);

                var count = SectionRenderDataUnsafe.getElementCount(data, facing);
                offset += (count / 6) * 4; // convert elements back into vertices
            }
        }
    }

    public long getDataPointer(int section) {
        return SectionRenderDataUnsafe.heapPointer(this.pArray, section);
    }
}