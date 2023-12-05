package me.jellysquid.mods.sodium.client.render.chunk.translucent_sorting.bsp_tree;

import org.joml.Vector3fc;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import me.jellysquid.mods.sodium.client.model.quad.properties.ModelQuadFacing;
import me.jellysquid.mods.sodium.client.render.chunk.translucent_sorting.TQuad;
import me.jellysquid.mods.sodium.client.render.chunk.translucent_sorting.TopoGraphSorting;
import me.jellysquid.mods.sodium.client.util.NativeBuffer;
import net.minecraft.util.math.ChunkSectionPos;

/**
 * A node in the BSP tree. The BSP tree is made up of nodes that split quads
 * into groups on either side of a plane and those that lie on the plane.
 * There's also leaf nodes that contain one or more quads.
 * 
 * Implementation note:
 * - Doing a convex box test doesn't seem to bring a performance boost, even if
 * it does trigger sometimes with man-made structures. The multi partition node
 * probably does most of the work already.
 */
public abstract class BSPNode {

    abstract void collectSortedQuads(BSPSortState sortState, Vector3fc cameraPos);

    public void collectSortedQuads(NativeBuffer nativeBuffer, Vector3fc cameraPos) {
        this.collectSortedQuads(new BSPSortState(nativeBuffer), cameraPos);
    }

    public static BSPResult buildBSP(TQuad[] quads, ChunkSectionPos sectionPos, BSPNode oldRoot) {
        // throw if there's too many quads
        InnerPartitionBSPNode.validateQuadCount(quads.length);

        // create a workspace and then the nodes figure out the recursive building.
        // throws if the BSP can't be built, null if none is necessary
        var workspace = new BSPWorkspace(quads, sectionPos);

        // initialize the indexes to all quads
        int[] initialIndexes = new int[quads.length];
        for (int i = 0; i < quads.length; i++) {
            initialIndexes[i] = i;
        }
        var allIndexes = new IntArrayList(initialIndexes);

        var rootNode = BSPNode.build(workspace, allIndexes, -1, oldRoot);
        var result = workspace.result;
        result.rootNode = rootNode;
        return result;
    }

    static BSPNode build(BSPWorkspace workspace, IntArrayList indexes, int depth, BSPNode oldNode) {
        depth++;

        // pick which type of node to create for the given workspace
        if (indexes.isEmpty()) {
            return null;
        } else if (indexes.size() == 1) {
            return new LeafSingleBSPNode(indexes.getInt(0));
        } else if (indexes.size() == 2) {
            var quadIndexA = indexes.getInt(0);
            var quadIndexB = indexes.getInt(1);
            var quadA = workspace.quads[quadIndexA];
            var quadB = workspace.quads[quadIndexB];

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
                return new LeafDoubleBSPNode(quadIndexA, quadIndexB);
            }
        }

        return InnerPartitionBSPNode.build(workspace, indexes, depth, oldNode);
    }
}
