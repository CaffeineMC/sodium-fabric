package me.jellysquid.mods.sodium.client.render.chunk.translucent_sorting.bsp_tree;

import org.joml.Vector3fc;

/**
 * A leaf node of a BSP tree that contains a single quad.
 */
class LeafSingleBSPNode extends BSPNode {
    private final int quad;

    LeafSingleBSPNode(int quad) {
        this.quad = quad;
    }

    @Override
    void collectSortedQuads(BSPSortState sortState, Vector3fc cameraPos) {
        sortState.writeIndex(this.quad);
    }
}
