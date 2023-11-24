package me.jellysquid.mods.sodium.client.render.chunk.translucent_sorting.bsp_tree;

import java.nio.IntBuffer;

import org.joml.Vector3fc;

import me.jellysquid.mods.sodium.client.render.chunk.translucent_sorting.data.TranslucentData;

class LeafSingleBSPNode extends BSPNode {
    private final int quad;

    LeafSingleBSPNode(int quad) {
        this.quad = quad;
    }

    @Override
    public void collectSortedQuads(IntBuffer indexBuffer, Vector3fc cameraPos) {
        TranslucentData.writeQuadVertexIndexes(indexBuffer, this.quad);
    }
}
