package net.caffeinemc.sodium.render.chunk.draw;

import net.caffeinemc.sodium.render.chunk.passes.ChunkRenderPass;

/**
 * The chunk render backend takes care of managing the graphics resource state of chunk render containers. This includes
 * the handling of uploading their data to the graphics card and rendering responsibilities.
 */
public interface ChunkRenderer {
    /**
     * Renders the given chunk render list to the active framebuffer.
     * @param renderLists An iterator over the list of chunks to be rendered
     * @param renderPass The block render pass to execute
     * @param matrices The camera matrices to use for rendering
     */
    void render(ChunkPrep.PreparedRenderList renderLists, ChunkRenderPass renderPass, ChunkRenderMatrices matrices);

    /**
     * Deletes this render backend and any resources attached to it.
     */
    void delete();
}
