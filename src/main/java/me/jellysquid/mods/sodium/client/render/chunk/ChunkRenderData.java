package me.jellysquid.mods.sodium.client.render.chunk;

import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import me.jellysquid.mods.sodium.client.render.vertex.BufferUploadData;
import net.minecraft.client.render.RenderLayer;

public interface ChunkRenderData {
    void destroy();

    void uploadMeshes(Object2ObjectMap<RenderLayer, BufferUploadData> layers);

    void deleteMeshes();
}
