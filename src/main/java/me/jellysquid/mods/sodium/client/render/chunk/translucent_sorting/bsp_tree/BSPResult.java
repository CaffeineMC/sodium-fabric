package me.jellysquid.mods.sodium.client.render.chunk.translucent_sorting.bsp_tree;

import me.jellysquid.mods.sodium.client.render.chunk.translucent_sorting.trigger.GeometryPlanes;

public class BSPResult extends GeometryPlanes {
    private BSPNode rootNode;

    public BSPNode getRootNode() {
        return this.rootNode;
    }

    public void setRootNode(BSPNode rootNode) {
        this.rootNode = rootNode;
    }
}
