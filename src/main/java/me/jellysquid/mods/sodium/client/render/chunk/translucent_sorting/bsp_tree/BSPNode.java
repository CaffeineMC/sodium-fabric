package me.jellysquid.mods.sodium.client.render.chunk.translucent_sorting.bsp_tree;

import java.nio.IntBuffer;

import org.joml.Vector3fc;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import me.jellysquid.mods.sodium.client.render.chunk.translucent_sorting.TQuad;
import net.minecraft.util.math.ChunkSectionPos;

/**
 * A node in the BSP tree. The BSP tree is made up of nodes that split quads
 * into groups on either side of a plane and those that lie on the plane.
 * There's also leaf nodes that contain one or more quads.
 */
public abstract class BSPNode {
    public abstract void collectSortedQuads(IntBuffer indexBuffer, Vector3fc cameraPos);

    public static BSPResult buildBSP(TQuad[] quads, ChunkSectionPos sectionPos) {
        // create a workspace and then the nodes figure out the recursive building.
        // throws if the BSP can't be built, null if none is necessary
        var workspace = new BSPWorkspace(quads, sectionPos);
        var rootNode = BSPNode.build(workspace);
        var result = workspace.result;
        result.rootNode = rootNode;
        return result;
    }

    static BSPNode build(BSPWorkspace workspace) {
        // pick which type of node to create for the given workspace
        // TODO: add more heuristics here
        if (workspace.indexes.isEmpty()) {
            return null;
        } else if (workspace.indexes.size() == 1) {
            return new LeafSingleBSPNode(workspace.indexes.getInt(0));
        } else {
            return InnerPartitionBSPNode.build(workspace);
        }
    }

    static BSPNode buildWithIndexes(BSPWorkspace workspace, IntArrayList indexes) {
        workspace.indexes = indexes;
        return build(workspace);
    }
}
