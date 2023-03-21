package me.jellysquid.mods.sodium.client.render.chunk.graph;

import me.jellysquid.mods.sodium.client.render.chunk.data.BuiltSectionInfo;
import me.jellysquid.mods.sodium.client.render.chunk.region.RenderRegion;

public class GraphNodeStorage {
    public final long[] nodes = new long[RenderRegion.REGION_SIZE];

    public void setData(int index, BuiltSectionInfo data) {
        if (data == null) {
            this.setNode(index, 0);
        } else {
            this.setNode(index, GraphNode.pack(VisibilityEncoding.encode(data.getOcclusionData()), data.getFlags() | GraphNodeFlags.IS_LOADED));
        }
    }

    public long getNode(int index) {
        return this.nodes[index];
    }

    public void setNode(int index, long node) {
        this.nodes[index] = node;
    }
}
