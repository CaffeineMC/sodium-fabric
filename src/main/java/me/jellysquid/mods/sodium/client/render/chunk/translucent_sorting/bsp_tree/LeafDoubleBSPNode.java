package me.jellysquid.mods.sodium.client.render.chunk.translucent_sorting.bsp_tree;

import java.nio.IntBuffer;

import org.joml.Vector3fc;

import me.jellysquid.mods.sodium.client.render.chunk.translucent_sorting.data.TranslucentData;

public class LeafDoubleBSPNode extends BSPNode {
    private final int quadA;
    private final int quadB;

    LeafDoubleBSPNode(int quadA, int quadB) {
        this.quadA = quadA;
        this.quadB = quadB;
    }

    @Override
    public void collectSortedQuads(IntBuffer indexBuffer, Vector3fc cameraPos) {
        TranslucentData.writeQuadVertexIndexes(indexBuffer, this.quadA);
        TranslucentData.writeQuadVertexIndexes(indexBuffer, this.quadB);
    }
}
