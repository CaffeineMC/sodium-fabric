package me.jellysquid.mods.sodium.client.render.backends.shader.lcb;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import me.jellysquid.mods.sodium.client.gl.SodiumVertexFormats;
import me.jellysquid.mods.sodium.client.gl.array.GlVertexArray;
import me.jellysquid.mods.sodium.client.gl.attribute.GlVertexFormat;
import me.jellysquid.mods.sodium.client.gl.buffer.GlBuffer;
import me.jellysquid.mods.sodium.client.gl.buffer.GlMutableBuffer;
import me.jellysquid.mods.sodium.client.gl.buffer.VertexData;
import me.jellysquid.mods.sodium.client.gl.func.GlFunctions;
import me.jellysquid.mods.sodium.client.gl.memory.BufferBlock;
import me.jellysquid.mods.sodium.client.gl.memory.BufferSegment;
import me.jellysquid.mods.sodium.client.render.backends.shader.AbstractShaderChunkRenderBackend;
import me.jellysquid.mods.sodium.client.render.chunk.ChunkBuildResult;
import me.jellysquid.mods.sodium.client.render.chunk.ChunkMesh;
import me.jellysquid.mods.sodium.client.render.chunk.ChunkRenderContainer;
import me.jellysquid.mods.sodium.client.render.chunk.ChunkRenderData;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkSectionPos;
import org.lwjgl.opengl.GL15;

import java.util.Iterator;

/**
 * Shader-based chunk renderer which makes use of a custom memory allocator on top of large buffer objects to allow
 * for draw call batching without buffer switching.
 *
 * The biggest bottleneck after setting up vertex attribute state is the sheer number of buffer switches and draw calls
 * being performed. In vanilla, the game uses one buffer for every chunk section, which means we need to bind, setup,
 * and draw every chunk individually.
 *
 * In order to reduce the number of these calls, we need to firstly reduce the number of buffer switches. We do this
 * through sub-dividing the world into larger "chunk regions" which then have one large buffer object in OpenGL. From
 * here, we can allocate slices of this buffer to each individual chunk and then only bind it once before drawing. Then,
 * our draw calls can simply point to individual sections within the buffer by manipulating the offset and count
 * parameters.
 *
 * However, an unfortunate consequence is that if we run out of space in a buffer, we need to re-allocate the entire
 * storage, which can take a ton of time! With old OpenGL 2.1 code, the only way to do this would be to copy the buffer's
 * memory from the graphics card over the host bus into CPU memory, allocate a new buffer, and then copy it back over
 * the bus and into graphics card. For reasons that should be obvious, this is extremely inefficient and requires the
 * CPU and GPU to be synchronized.
 *
 * If we make use of more modern OpenGL 3.0 features, we can avoid this transfer over the memory bus and instead just
 * perform the copy between buffers in GPU memory with the aptly named "copy buffer" function. It's still not blazing
 * fast, but it's much better than what we're stuck with in older versions. We can help prevent these re-allocations by
 * sizing our buffers to be a bit larger than what we expect all the chunk data to be, but this wastes memory.
 *
 * In the initial implementation, this solution worked fine enough, but the amount of time being spent on uploading
 * chunks to the large buffers was now a magnitude more than what it was before all of this and it made chunk updates
 * *very* slow. It took some tinkering to figure out what was going wrong here, but at least on the NVIDIA drivers, it
 * seems that updating sub-regions of buffer memory hits some kind of slow path. A workaround for this problem is to
 * create a scratch buffer object and upload the chunk data there *first*, re-allocating the storage each time. Then,
 * you can copy the contents of the scratch buffer into the chunk region buffer, rise and repeat. I'm not happy with
 * this solution, but it performs surprisingly well across all hardware I tried.
 *
 * While eliminating most of the buffer switches provides a considerable reduction to CPU overhead, it's still possible
 * to reduce it further as we're still issuing one draw call per section. Fortunately, OpenGL 1.4+ provides an easy way
 * to work around this issue with glMultiDrawArrays. This function allows us to perform multiple draws with one call by
 * passing pointers to an array of offsets and counts, and is fairly easy to translate the existing code to use.
 *
 * With both of these changes, the amount of CPU time taken by rendering chunks linearly decreases with the reduction
 * in buffer bind/setup/draw calls. Using the default settings of 4x2x4 chunk region buffers, the number of calls can be
 * reduced up to a factor of ~32x.
 */
public class ShaderLCBChunkRenderBackend extends AbstractShaderChunkRenderBackend<ShaderLCBRenderState> {
    private final ChunkRegionManager bufferManager;
    private final GlMutableBuffer uploadBuffer;

    private final ObjectList<ChunkRegion> pendingBatches = new ObjectArrayList<>();

    public ShaderLCBChunkRenderBackend(GlVertexFormat<SodiumVertexFormats.ChunkMeshAttribute> format) {
        super(format);

        this.bufferManager = new ChunkRegionManager();
        this.uploadBuffer = new GlMutableBuffer(GL15.GL_STREAM_COPY);
    }

    @Override
    public void upload(Iterator<ChunkBuildResult<ShaderLCBRenderState>> queue) {
        GlMutableBuffer uploadBuffer = this.uploadBuffer;
        uploadBuffer.bind(GL15.GL_ARRAY_BUFFER);

        BufferBlock prevBlock = null;

        while (queue.hasNext()) {
            ChunkBuildResult<ShaderLCBRenderState> result = queue.next();

            ChunkRenderContainer<ShaderLCBRenderState> render = result.render;
            ChunkRenderData data = result.data;

            render.resetRenderStates();
            render.setData(data);

            for (ChunkMesh mesh : data.getMeshes()) {
                ChunkSectionPos pos = render.getChunkPos();

                ChunkRegion region = this.bufferManager.createRegion(pos);
                BufferBlock block = region.getBuffer();

                if (prevBlock != block) {
                    if (prevBlock != null) {
                        prevBlock.endUploads();
                    }

                    block.beginUpload();

                    prevBlock = block;
                }

                VertexData upload = mesh.takePendingUpload();
                uploadBuffer.upload(GL15.GL_ARRAY_BUFFER, upload);

                BufferSegment segment = block.upload(GL15.GL_ARRAY_BUFFER, 0, upload.buffer.capacity());

                render.setRenderState(mesh.getRenderPass(), new ShaderLCBRenderState(region, segment, this.vertexFormat));
            }
        }

        if (prevBlock != null) {
            prevBlock.endUploads();
        }

        uploadBuffer.invalidate(GL15.GL_ARRAY_BUFFER);
        uploadBuffer.unbind(GL15.GL_ARRAY_BUFFER);
    }

    @Override
    public void render(Iterator<ShaderLCBRenderState> renders, MatrixStack matrixStack, double x, double y, double z) {
        this.bufferManager.cleanup();

        this.setupBatches(renders);
        this.begin(matrixStack);

        int chunkX = (int) (x / 16.0D);
        int chunkY = (int) (y / 16.0D);
        int chunkZ = (int) (z / 16.0D);

        this.activeProgram.setupModelViewMatrix(matrixStack, x % 16.0D, y % 16.0D, z % 16.0D);

        GlVertexArray prevArray = null;

        for (ChunkRegion region : this.pendingBatches) {
            this.activeProgram.setupChunk(region.getOrigin(), chunkX, chunkY, chunkZ);

            prevArray = region.drawBatch(this.activeProgram.attributes);
        }

        if (prevArray != null) {
            prevArray.unbind();
        }

        this.pendingBatches.clear();

        this.end(matrixStack);
    }

    private void setupBatches(Iterator<ShaderLCBRenderState> renders) {
        while (renders.hasNext()) {
            ShaderLCBRenderState state = renders.next();

            if (state != null) {
                ChunkRegion region = state.getRegion();

                if (region.isBatchEmpty()) {
                    this.pendingBatches.add(region);
                }

                region.addToBatch(state);
            }
        }
    }

    @Override
    public Class<ShaderLCBRenderState> getRenderStateType() {
        return ShaderLCBRenderState.class;
    }

    @Override
    protected ShaderLCBRenderState createRenderState(GlBuffer buffer, ChunkRenderContainer<ShaderLCBRenderState> render) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void delete() {
        super.delete();

        this.bufferManager.delete();
        this.uploadBuffer.delete();
    }

    @Override
    public BlockPos getRenderOffset(ChunkSectionPos pos) {
        return this.bufferManager.getRenderOffset(pos);
    }

    public static boolean isSupported() {
        return GlVertexArray.isSupported() && GlFunctions.isBufferCopySupported();
    }
}
