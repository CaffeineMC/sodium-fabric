package me.jellysquid.mods.sodium.client.render.chunk.translucent_sorting.bsp_tree;

import me.jellysquid.mods.sodium.client.model.quad.properties.ModelQuadFacing;
import me.jellysquid.mods.sodium.client.render.chunk.translucent_sorting.AccumulationGroup;
import me.jellysquid.mods.sodium.client.render.chunk.translucent_sorting.TQuad;
import net.minecraft.util.math.ChunkSectionPos;

/**
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
        var accGroups = result.getAlignedDistancesOrCreate();
        this.addOneSidedPlane(accGroups, axis, distance);
        this.addOneSidedPlane(accGroups, axis + 3, -distance);
    }

    void addOneSidedPlane(AccumulationGroup[] accGroups, int direction, float distance) {
        var accGroup = accGroups[direction];
        if (accGroup == null) {
            accGroup = new AccumulationGroup(
                    this.sectionPos, ModelQuadFacing.NORMALS[direction], direction);
            accGroups[direction] = accGroup;
        }
        accGroup.addPlaneMember(distance);
    }
}
