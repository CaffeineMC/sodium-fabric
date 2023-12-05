package me.jellysquid.mods.sodium.client.render.chunk.translucent_sorting.bsp_tree;

import org.joml.Vector3fc;

class LeafMultiBSPNode extends BSPNode {
    private final int[] quads;

    LeafMultiBSPNode(int[] quads) {
        this.quads = quads;
    }

    @Override
    void collectSortedQuads(BSPSortState sortState, Vector3fc cameraPos) {
        sortState.writeIndexes(this.quads);
    }
}
