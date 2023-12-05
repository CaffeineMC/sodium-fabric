package me.jellysquid.mods.sodium.client.render.chunk.translucent_sorting.bsp_tree;

import org.joml.Vector3fc;

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
