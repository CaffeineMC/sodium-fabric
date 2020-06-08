package me.jellysquid.mods.sodium.client.render.backends;

import me.jellysquid.mods.sodium.client.gl.SodiumVertexFormats.ChunkMeshAttribute;
import me.jellysquid.mods.sodium.client.gl.attribute.GlVertexFormat;
import me.jellysquid.mods.sodium.client.render.chunk.ChunkBuildResult;
import me.jellysquid.mods.sodium.client.render.chunk.ChunkRenderContainer;
import me.jellysquid.mods.sodium.client.render.layer.BlockRenderPass;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkSectionPos;

import java.util.Iterator;

/**
 * The chunk render backend takes care of managing the graphics resource state of chunk render containers. This includes
 * the handling of uploading their data to the graphics card and rendering responsibilities.
 * @param <T> The type of graphics state to be used in chunk render containers
 */
public interface ChunkRenderBackend<T extends ChunkGraphicsState> {
    /**
     * Drains the iterator of items and processes each build task's result serially. After this method returns, all
     * drained results should be processed.
     */
    void upload(Iterator<ChunkBuildResult<T>> queue);

    /**
     * Renders the given chunk render list to the active framebuffer.
     * @param pass
     * @param renders The render list
     * @param matrixStack The current matrix stack containing the model-view matrices for rendering
     * @param x The x-position of the camera in world space
     * @param y The y-position of the camera in world space
     * @param z The z-position of the camera in world space
     */
    void render(BlockRenderPass pass, Iterator<ChunkRenderContainer<T>> renders, MatrixStack matrixStack, double x, double y, double z);

    /**
     * Deletes this render backend and any resources attached to it.
     */
    void delete();

    /**
     * Returns the vertex format used by this chunk render backend for rendering meshes.
     */
    GlVertexFormat<ChunkMeshAttribute> getVertexFormat();

    /**
     * Returns the block rendering offset for a given chunk section to be used for building chunk meshes. This should
     * be changed by implementations which need control over the location of a chunk mesh in world space.
     * @param pos The position of the chunk mesh
     */
    default BlockPos getRenderOffset(ChunkSectionPos pos) {
        return new BlockPos(pos.getX() << 4, pos.getY() << 4, pos.getZ() << 4);
    }
}
