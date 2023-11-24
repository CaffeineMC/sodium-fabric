package me.jellysquid.mods.sodium.client.render.chunk.translucent_sorting.bsp_tree;

import java.nio.IntBuffer;

import org.joml.Vector3fc;

import me.jellysquid.mods.sodium.client.render.chunk.translucent_sorting.data.TranslucentData;

class LeafMultiBSPNode extends BSPNode {
    private final int[] quads;

    LeafMultiBSPNode(int[] quads) {
        this.quads = quads;
    }

    @Override
    public void collectSortedQuads(IntBuffer indexBuffer, Vector3fc cameraPos) {
        TranslucentData.writeQuadVertexIndexes(indexBuffer, this.quads);
    }
}
