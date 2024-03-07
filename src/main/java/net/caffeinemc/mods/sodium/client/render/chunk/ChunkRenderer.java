package net.caffeinemc.mods.sodium.client.render.chunk;

import net.caffeinemc.mods.sodium.client.gl.device.CommandList;
import net.caffeinemc.mods.sodium.client.render.chunk.lists.ChunkRenderListIterable;
import net.caffeinemc.mods.sodium.client.render.chunk.terrain.TerrainRenderPass;
import net.caffeinemc.mods.sodium.client.render.viewport.CameraTransform;

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
     * @param renderLists The collection of render lists
     * @param pass        The block render pass to execute
     * @param camera      The camera context containing chunk offsets for the current render
     */
    void render(ChunkRenderMatrices matrices, CommandList commandList, ChunkRenderListIterable renderLists, TerrainRenderPass pass, CameraTransform camera);

    /**
     * Deletes this render backend and any resources attached to it.
     */
    void delete(CommandList commandList);
}
