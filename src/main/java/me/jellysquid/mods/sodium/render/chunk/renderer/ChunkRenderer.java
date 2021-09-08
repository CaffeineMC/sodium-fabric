package me.jellysquid.mods.sodium.render.chunk.renderer;

import me.jellysquid.mods.sodium.model.vertex.type.ChunkVertexType;
import me.jellysquid.mods.sodium.render.chunk.context.ChunkCameraContext;
import me.jellysquid.mods.sodium.render.chunk.ChunkRenderList;
import me.jellysquid.mods.sodium.render.chunk.context.ChunkRenderMatrices;
import me.jellysquid.mods.sodium.render.chunk.passes.BlockRenderPass;
import me.jellysquid.mods.thingl.device.RenderDevice;

/**
 * The chunk render backend takes care of managing the graphics resource state of chunk render containers. This includes
 * the handling of uploading their data to the graphics card and rendering responsibilities.
 */
public interface ChunkRenderer {
    /**
     * Renders the given chunk render list to the active framebuffer.
     * @param device
     * @param matrices The camera matrices to use for rendering
     * @param renders An iterator over the list of chunks to be rendered
     * @param pass The block render pass to execute
     * @param camera The camera context containing chunk offsets for the current render
     */
    void render(RenderDevice device, ChunkRenderMatrices matrices, ChunkRenderList renders, BlockRenderPass pass, ChunkCameraContext camera);

    /**
     * Deletes this render backend and any resources attached to it.
     */
    void delete();

    /**
     * Returns the vertex format used by this chunk render backend for rendering meshes.
     */
    ChunkVertexType getVertexType();
}
