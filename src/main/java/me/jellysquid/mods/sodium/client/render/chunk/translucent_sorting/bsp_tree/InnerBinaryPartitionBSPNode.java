package me.jellysquid.mods.sodium.client.render.chunk.translucent_sorting.bsp_tree;

import java.nio.IntBuffer;

import org.joml.Vector3fc;

import me.jellysquid.mods.sodium.client.render.chunk.translucent_sorting.data.TranslucentData;

class InnerBinaryPartitionBSPNode extends InnerPartitionBSPNode {
    private final float planeDistance;

    // side towards which the normal points
    private final BSPNode inside; // nullable
    private final BSPNode outside; // nullable
    private final int[] onPlaneQuads;

    InnerBinaryPartitionBSPNode(float planeDistance, Vector3fc planeNormal,
            BSPNode inside, BSPNode outside, int[] onPlaneQuads) {
        super(planeNormal);
        this.planeDistance = planeDistance;
        this.inside = inside;
        this.outside = outside;
        this.onPlaneQuads = onPlaneQuads;
    }

    private void collectInside(IntBuffer indexBuffer, Vector3fc cameraPos) {
        if (this.inside != null) {
            this.inside.collectSortedQuads(indexBuffer, cameraPos);
        }
    }

    private void collectOutside(IntBuffer indexBuffer, Vector3fc cameraPos) {
        if (this.outside != null) {
            this.outside.collectSortedQuads(indexBuffer, cameraPos);
        }
    }

    @Override
    public void collectSortedQuads(IntBuffer indexBuffer, Vector3fc cameraPos) {
        var cameraInside = this.planeNormal.dot(cameraPos) < this.planeDistance;
        if (cameraInside) {
            this.collectOutside(indexBuffer, cameraPos);
        } else {
            this.collectInside(indexBuffer, cameraPos);
        }
        if (this.onPlaneQuads != null) {
            TranslucentData.writeQuadVertexIndexes(indexBuffer, this.onPlaneQuads);
        }
        if (cameraInside) {
            this.collectInside(indexBuffer, cameraPos);
        } else {
            this.collectOutside(indexBuffer, cameraPos);
        }
    }
}
