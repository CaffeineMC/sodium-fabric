package me.jellysquid.mods.sodium.render.chunk.draw;

import me.jellysquid.mods.sodium.opengl.device.RenderDevice;
import me.jellysquid.mods.sodium.render.terrain.format.TerrainVertexType;
import me.jellysquid.mods.sodium.render.chunk.passes.ChunkRenderPass;

/**
 * The chunk render backend takes care of managing the graphics resource state of chunk render containers. This includes
 * the handling of uploading their data to the graphics card and rendering responsibilities.
 */
public interface ChunkRenderer {
    /**
     * Renders the given chunk render list to the active framebuffer.
     * @param matrices The camera matrices to use for rendering
     * @param device The device which the scene will be rendered on
     * @param list An iterator over the list of chunks to be rendered
     * @param pass The block render pass to execute
     * @param camera The camera context containing chunk offsets for the current render
     */
    void render(ChunkRenderMatrices matrices, RenderDevice device, ChunkRenderList list, ChunkRenderPass pass, ChunkCameraContext camera);

    /**
     * Deletes this render backend and any resources attached to it.
     */
    void delete();

    void flush();
}
