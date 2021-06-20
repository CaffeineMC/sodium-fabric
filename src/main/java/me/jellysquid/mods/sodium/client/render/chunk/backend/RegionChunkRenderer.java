package me.jellysquid.mods.sodium.client.render.chunk.backend;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import it.unimi.dsi.fastutil.objects.Reference2ObjectLinkedOpenHashMap;
import me.jellysquid.mods.sodium.client.gl.device.RenderDevice;
import me.jellysquid.mods.sodium.client.model.vertex.type.ChunkVertexType;
import me.jellysquid.mods.sodium.client.render.chunk.RenderChunk;
import me.jellysquid.mods.sodium.client.render.RenderRegion;
import me.jellysquid.mods.sodium.client.render.chunk.shader.ShaderChunkRenderer;

import java.util.List;
import java.util.Map;

public abstract class RegionChunkRenderer extends ShaderChunkRenderer {
    public RegionChunkRenderer(RenderDevice device, ChunkVertexType vertexType) {
        super(device, vertexType);
    }

    protected Map<RenderRegion, List<RenderChunk>> sortRenders(ObjectList<RenderChunk> renders) {
        Map<RenderRegion, List<RenderChunk>> lists = new Reference2ObjectLinkedOpenHashMap<>();

        for (RenderChunk render : renders) {
            List<RenderChunk> list = lists.get(render.getRegion());

            if (list == null) {
                lists.put(render.getRegion(), list = new ObjectArrayList<>(RenderRegion.REGION_SIZE));
            }

            list.add(render);
        }

        return lists;
    }
}
