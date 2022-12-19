package net.caffeinemc.sodium.render.chunk.draw;

import net.caffeinemc.sodium.render.chunk.passes.ChunkRenderPass;

/**
 * The chunk section backend takes care of managing the graphics resource state of chunk section containers. This includes
 * the handling of uploading their data to the graphics card and rendering responsibilities.
 */
public interface ChunkRenderer {
    
    /**
     * Creates and sets the current section lists for all section passes using the given inputs.
     * These will be used by the {@link #render(ChunkRenderPass, ChunkRenderMatrices, int) section}
     * function until this is called again.
     *
     * @param lists the sorted lists used to section the terrain
     * @param frameIndex  The monotonic index of the current frame being rendered
     */
    void createRenderLists(SortedTerrainLists lists, int frameIndex);
    
    /**
     * Renders the last created section list for the current pass to the active framebuffer.
     * If the section pass does not have commands generated for it, the method will do nothing.
     *
     * @param renderPass The section pass to section
     * @param matrices The camera matrices to use for rendering
     * @param frameIndex The monotonic index of the current frame being rendered
     */
    void render(ChunkRenderPass renderPass, ChunkRenderMatrices matrices, int frameIndex);
    
    /**
     * Deletes this section backend and any resources attached to it.
     */
    void delete();
    
    int getDeviceBufferObjects();
    
    long getDeviceUsedMemory();
    
    long getDeviceAllocatedMemory();
    
    String getDebugName();
}
