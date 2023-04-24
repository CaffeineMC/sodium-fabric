package me.jellysquid.mods.sodium.client.render.chunk;

import me.jellysquid.mods.sodium.client.gl.device.CommandList;
import me.jellysquid.mods.sodium.client.render.chunk.lists.ChunkRenderList;
import me.jellysquid.mods.sodium.client.render.chunk.terrain.TerrainRenderPass;
import me.jellysquid.mods.sodium.client.render.chunk.region.RenderRegionManager;

/**
 * The chunk render backend takes care of managing the graphics resource state of chunk render containers. This includes
 * the handling of uploading their data to the graphics card and rendering responsibilities.
 */
public interface ChunkRenderer {
    /**
     * Renders the given chunk render list to the active framebuffer.
     *
     * @param matrices    The camera matrices to use for rendering
     * @param commandList The command list which OpenGL commands should be serialized to
     * @param regions     The region storage which stores section graphics state and other information
     * @param renderList  An iterator over the list of chunks to be rendered
     * @param pass        The block render pass to execute
     * @param camera      The camera context containing chunk offsets for the current render
     */
    void render(ChunkRenderMatrices matrices, CommandList commandList, RenderRegionManager regions, ChunkRenderList renderList, TerrainRenderPass pass, ChunkCameraContext camera);

    /**
     * Deletes this render backend and any resources attached to it.
     */
    void delete(CommandList commandList);
}
