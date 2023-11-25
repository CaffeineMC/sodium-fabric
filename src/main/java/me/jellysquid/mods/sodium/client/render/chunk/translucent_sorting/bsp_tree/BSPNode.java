package me.jellysquid.mods.sodium.client.render.chunk.translucent_sorting.bsp_tree;

import java.nio.IntBuffer;

import org.joml.Vector3fc;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import me.jellysquid.mods.sodium.client.model.quad.properties.ModelQuadFacing;
import me.jellysquid.mods.sodium.client.render.chunk.translucent_sorting.TQuad;
import me.jellysquid.mods.sodium.client.render.chunk.translucent_sorting.TopoGraphSorting;
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
        }

        // special case two quads
        var indexes = workspace.indexes;
        if (indexes.size() == 2) {
            var quadA = workspace.quads[indexes.getInt(0)];
            var quadB = workspace.quads[indexes.getInt(1)];

            // check for coplanar or mutually invisible quads
            var facingA = quadA.facing();
            var facingB = quadB.facing();
            var normalA = quadA.normal();
            var normalB = quadB.normal();
            if (
            // coplanar quads
            ((facingA == ModelQuadFacing.UNASSIGNED || facingB == ModelQuadFacing.UNASSIGNED)
                    ? // opposite normal (distance irrelevant)
                    normalA.x() == -normalB.x()
                            && normalA.y() == -normalB.y()
                            && normalA.z() == -normalB.z()
                            // same normal and same distance
                            || normalA.equals(quadB.normal())
                                    && normalA.dot(quadA.center()) == quadB.normal().dot(quadB.center())
                    // aligned same distance
                    : quadA.extents()[facingA.ordinal()] == quadB.extents()[facingB.ordinal()])
                    // facing away from eachother
                    || facingA == facingB.getOpposite()
                    // otherwise mutually invisible
                    || facingA != ModelQuadFacing.UNASSIGNED
                            && facingB != ModelQuadFacing.UNASSIGNED
                            && !TopoGraphSorting.orthogonalQuadVisibleThrough(quadA, quadB)
                            && !TopoGraphSorting.orthogonalQuadVisibleThrough(quadB, quadA)) {
                return new LeafMultiBSPNode(indexes.toIntArray());
            }
        }

        return InnerPartitionBSPNode.build(workspace);
    }

    static BSPNode buildWithIndexes(BSPWorkspace workspace, IntArrayList indexes) {
        workspace.indexes = indexes;
        return build(workspace);
    }
}
