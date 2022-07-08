package net.caffeinemc.sodium.render.chunk.draw;

import net.caffeinemc.sodium.render.chunk.passes.ChunkRenderPass;

/**
 * The chunk render backend takes care of managing the graphics resource state of chunk render containers. This includes
 * the handling of uploading their data to the graphics card and rendering responsibilities.
 */
public interface ChunkRenderer {
    
    /**
     * Creates and sets the current render lists for all render passes using the given inputs.
     * These will be used by the {@link #render(ChunkRenderPass, ChunkRenderMatrices, int) render}
     * function until this is called again.
     *
     * @param chunks the list of sorted chunks to create the render lists from
     * @param camera the camera of the player
     * @param frameIndex  The monotonic index of the current frame being rendered
     */
    void createRenderLists(SortedChunkLists chunks, ChunkCameraContext camera, int frameIndex);
    
    /**
     * Renders the last created render list for the current pass to the active framebuffer.
     * If the render pass does not have commands generated for it, the method will do nothing.
     *
     * @param renderPass The render pass to render
     * @param matrices The camera matrices to use for rendering
     * @param frameIndex The monotonic index of the current frame being rendered
     */
    void render(ChunkRenderPass renderPass, ChunkRenderMatrices matrices, int frameIndex);
    
    /**
     * Deletes this render backend and any resources attached to it.
     */
    void delete();
    
    int getDeviceBufferObjects();
    
    long getDeviceUsedMemory();
    
    long getDeviceAllocatedMemory();
    
    String getDebugName();
}
