package me.jellysquid.mods.sodium.client.render.chunk.translucent_sorting.bsp_tree;

import it.unimi.dsi.fastutil.ints.IntArrayList;
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

    /**
     * A list of all the quad indexes to process in the next build invocation.
     */
    IntArrayList indexes;

    /**
     * The current depth of the recursive BSP building process.
     */
    int depth = 1;

    BSPResult result = new BSPResult();

    BSPWorkspace(TQuad[] quads, ChunkSectionPos sectionPos) {
        this.quads = quads;
        this.sectionPos = sectionPos;

        // initialize the indexes to all quads
        int[] initialIndexes = new int[quads.length];
        for (int i = 0; i < quads.length; i++) {
            initialIndexes[i] = i;
        }
        this.indexes = new IntArrayList(initialIndexes);
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
