package me.jellysquid.mods.sodium.client.render.chunk.translucent_sorting.bsp_tree;

import me.jellysquid.mods.sodium.client.render.chunk.translucent_sorting.TQuad;
import me.jellysquid.mods.sodium.client.render.chunk.translucent_sorting.trigger.GeometryPlanes;
import net.minecraft.util.math.ChunkSectionPos;

/**
 * The BSP workspace holds the state during the BSP building process. (see also
 * BSPSortState) It brings a number of fixed parameters and receives partition
 * planes to return as part of the final result.
 * 
 * Implementation note: Storing the multi partition node's interval points in a
 * global array instead of making a new one at each tree level doesn't appear to
 * have any performance benefit.
 */
class BSPWorkspace {
    /**
     * All the quads in the section.
     */
    final TQuad[] quads;

    final ChunkSectionPos sectionPos;

    final BSPResult result = new BSPResult();

    final boolean prepareNodeReuse;

    BSPWorkspace(TQuad[] quads, ChunkSectionPos sectionPos, boolean prepareNodeReuse) {
        this.quads = quads;
        this.sectionPos = sectionPos;
        this.prepareNodeReuse = prepareNodeReuse;
    }

    // TODO: better bidirectional triggering: integrate bidirectionality in GFNI if
    // top-level topo sorting isn't used anymore (and only use half as much memory
    // by not storing it double)
    void addAlignedPartitionPlane(int axis, float distance) {
        result.addDoubleSidedPlane(this.sectionPos, axis, distance);
    }

    GeometryPlanes getGeometryPlanes() {
        return result;
    }
}
