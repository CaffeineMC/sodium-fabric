package me.jellysquid.mods.sodium.client.render.chunk.compile.buffers;

import me.jellysquid.mods.sodium.client.model.quad.properties.ModelQuadFacing;
import me.jellysquid.mods.sodium.client.render.chunk.terrain.material.Material;
import me.jellysquid.mods.sodium.client.render.chunk.vertex.builder.ChunkMeshBufferBuilder;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.texture.Sprite;

public interface ChunkModelBuilder {
    ChunkMeshBufferBuilder getVertexBuffer(ModelQuadFacing facing);

    void addSprite(Sprite sprite);

    /**
     * <b>This method should not be used unless absolutely necessary!</b> It exists only for compatibility purposes.
     * Prefer using the other methods in this class instead as they are more efficient.
     *
     * <p>Returns a {@link VertexConsumer} which adds geometry to this model builder using the given {@link Material}.
     * The returned vertex consumer expects quads and requires the position, color, texture, light, and normal
     * attributes to be provided for each vertex. The returned vertex consumer may be a reused object, so it must not be
     * stored or cached in any way by the caller.
     *
     * @param material the material that should be used for geometry pushed to the vertex consumer
     * @return the fallback vertex consumer which adds geometry to this model builder
     */
    VertexConsumer asFallbackVertexConsumer(Material material);
}
