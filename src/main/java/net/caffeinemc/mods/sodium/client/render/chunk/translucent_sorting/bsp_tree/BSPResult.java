package net.caffeinemc.mods.sodium.client.render.chunk.translucent_sorting.bsp_tree;

import net.caffeinemc.mods.sodium.client.render.chunk.translucent_sorting.trigger.GeometryPlanes;

/**
 * The result of a BSP building operation. Building a BSP returns the root node
 * along with the partition planes that need to be added to the trigger system.
 */
public class BSPResult extends GeometryPlanes {
    private BSPNode rootNode;

    public BSPNode getRootNode() {
        return this.rootNode;
    }

    public void setRootNode(BSPNode rootNode) {
        this.rootNode = rootNode;
    }
}
