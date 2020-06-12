package me.jellysquid.mods.sodium.client.render.chunk.backends.gl46;

import it.unimi.dsi.fastutil.objects.ObjectArrayFIFOQueue;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import me.jellysquid.mods.sodium.client.gl.SodiumVertexFormats;
import me.jellysquid.mods.sodium.client.gl.arena.GlBufferArena;
import me.jellysquid.mods.sodium.client.gl.arena.GlBufferRegion;
import me.jellysquid.mods.sodium.client.gl.array.GlVertexArray;
import me.jellysquid.mods.sodium.client.gl.attribute.GlVertexFormat;
import me.jellysquid.mods.sodium.client.gl.buffer.GlBuffer;
import me.jellysquid.mods.sodium.client.gl.buffer.GlMutableBuffer;
import me.jellysquid.mods.sodium.client.gl.buffer.VertexData;
import me.jellysquid.mods.sodium.client.gl.func.GlFunctions;
import me.jellysquid.mods.sodium.client.gl.util.VertexSlice;
import me.jellysquid.mods.sodium.client.render.chunk.ChunkCameraContext;
import me.jellysquid.mods.sodium.client.render.chunk.ChunkRenderContainer;
import me.jellysquid.mods.sodium.client.render.chunk.compile.ChunkBuildResult;
import me.jellysquid.mods.sodium.client.render.chunk.data.ChunkMeshData;
import me.jellysquid.mods.sodium.client.render.chunk.data.ChunkRenderData;
import me.jellysquid.mods.sodium.client.render.chunk.multidraw.ChunkMultiDrawBatch;
import me.jellysquid.mods.sodium.client.render.chunk.multidraw.ChunkRenderBackendMultidraw;
import me.jellysquid.mods.sodium.client.render.chunk.passes.BlockRenderPass;
import me.jellysquid.mods.sodium.client.render.chunk.region.ChunkRegion;
import me.jellysquid.mods.sodium.client.render.chunk.region.ChunkRegionManager;
import net.minecraft.client.util.math.MatrixStack;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL15;

import java.util.Iterator;
import java.util.List;

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
public class GL46ChunkRenderBackend extends ChunkRenderBackendMultidraw<LCBGraphicsState> {
    private final ChunkRegionManager<LCBGraphicsState> bufferManager;

    private final ObjectArrayFIFOQueue<ChunkRegion<LCBGraphicsState>> pendingBatches = new ObjectArrayFIFOQueue<>();
    private final ObjectArrayFIFOQueue<ChunkRegion<LCBGraphicsState>> pendingUploads = new ObjectArrayFIFOQueue<>();

    private final GlMutableBuffer uploadBuffer;

    public GL46ChunkRenderBackend(GlVertexFormat<SodiumVertexFormats.ChunkMeshAttribute> format) {
        super(format, ChunkRegionManager.BUFFER_SIZE);

        this.bufferManager = new ChunkRegionManager<>();
        this.uploadBuffer = new GlMutableBuffer(GL15.GL_STREAM_COPY);
    }

    @Override
    public void upload(Iterator<ChunkBuildResult<LCBGraphicsState>> queue) {
        this.setupUploadBatches(queue);

        GlMutableBuffer uploadBuffer = this.uploadBuffer;
        uploadBuffer.bind(GL15.GL_ARRAY_BUFFER);

        while (!this.pendingUploads.isEmpty()) {
            ChunkRegion<LCBGraphicsState> region = this.pendingUploads.dequeue();

            GlBufferArena arena = region.getBufferArena();
            arena.bind();

            ObjectArrayList<ChunkBuildResult<LCBGraphicsState>> uploadQueue = region.getUploadQueue();
            arena.ensureCapacity(getUploadQueuePayloadSize(uploadQueue));

            for (ChunkBuildResult<LCBGraphicsState> result : uploadQueue) {
                ChunkRenderContainer<LCBGraphicsState> render = result.render;
                ChunkRenderData data = result.data;

                LCBGraphicsState graphics = render.getGraphicsState();

                // De-allocate the existing buffer arena for this render
                // This will allow it to be cheaply re-allocated just below
                if (graphics != null) {
                    graphics.delete();
                }

                ChunkMeshData meshData = data.getMeshData();

                if (meshData.hasData()) {
                    VertexData upload = meshData.takePendingUpload();
                    uploadBuffer.upload(GL15.GL_ARRAY_BUFFER, upload);

                    GlBufferRegion segment = arena.upload(GL15.GL_ARRAY_BUFFER, 0, upload.buffer.capacity());

                    render.setGraphicsState(new LCBGraphicsState(region, segment, meshData, this.vertexFormat));
                } else {
                    render.setGraphicsState(null);
                }

                render.setData(data);
            }

            arena.unbind();
            uploadQueue.clear();
        }

        uploadBuffer.invalidate(GL15.GL_ARRAY_BUFFER);
        uploadBuffer.unbind(GL15.GL_ARRAY_BUFFER);
    }

    @Override
    public void render(BlockRenderPass pass, Iterator<ChunkRenderContainer<LCBGraphicsState>> renders, MatrixStack matrixStack, ChunkCameraContext camera) {
        super.begin(matrixStack);

        this.bufferManager.cleanup();
        this.setupDrawBatches(pass, renders, camera);

        GlVertexArray prevVao = null;

        while (!this.pendingBatches.isEmpty()) {
            ChunkRegion<?> region = this.pendingBatches.dequeue();

            GlVertexArray vao = region.getVertexArray();
            vao.bind();

            GlBuffer vbo = region.getBufferArena().getBuffer();

            // Check if the VAO's bindings need to be updated
            // This happens whenever the backing buffer object for the arena changes
            if (region.getPrevVbo() != vbo) {
                vbo.bind(GL15.GL_ARRAY_BUFFER);

                this.vertexFormat.bindVertexAttributes();
                this.vertexFormat.enableVertexAttributes();

                vbo.unbind(GL15.GL_ARRAY_BUFFER);

                region.setPrevVbo(vbo);
            }

            ChunkMultiDrawBatch batch = region.getDrawBatch();
            batch.end();

            this.activeProgram.uploadModelOffsetUniforms(batch.getUniformUploadBuffer());

            GL14.glMultiDrawArrays(GL11.GL_QUADS, batch.getIndicesBuffer(), batch.getLengthBuffer());

            prevVao = vao;
        }

        if (prevVao != null) {
            prevVao.unbind();
        }

        this.end(matrixStack);
    }

    private void setupUploadBatches(Iterator<ChunkBuildResult<LCBGraphicsState>> renders) {
        while (renders.hasNext()) {
            ChunkBuildResult<LCBGraphicsState> result = renders.next();

            ChunkRegion<LCBGraphicsState> region = this.bufferManager.createRegion(result.render.getChunkPos());
            ObjectArrayList<ChunkBuildResult<LCBGraphicsState>> uploadQueue = region.getUploadQueue();

            if (uploadQueue.isEmpty()) {
                this.pendingUploads.enqueue(region);
            }

            uploadQueue.add(result);
        }
    }

    private void setupDrawBatches(BlockRenderPass pass, Iterator<ChunkRenderContainer<LCBGraphicsState>> renders, ChunkCameraContext camera) {
        while (renders.hasNext()) {
            ChunkRenderContainer<LCBGraphicsState> render = renders.next();
            LCBGraphicsState state = render.getGraphicsState();

            if (state == null) {
                continue;
            }

            long slice = state.getSliceForLayer(pass);

            if (VertexSlice.isEmpty(slice)) {
                continue;
            }

            ChunkRegion<LCBGraphicsState> region = state.getRegion();
            ChunkMultiDrawBatch batch = region.getDrawBatch();

            if (!batch.isBuilding()) {
                batch.begin();

                this.pendingBatches.enqueue(region);
            }

            int start = VertexSlice.unpackFirst(slice);
            int count = VertexSlice.unpackCount(slice);

            float modelX = camera.getChunkModelOffset(render.getOriginX(), camera.blockOriginX, camera.originX);
            float modelY = camera.getChunkModelOffset(render.getOriginY(), camera.blockOriginY, camera.originY);
            float modelZ = camera.getChunkModelOffset(render.getOriginZ(), camera.blockOriginZ, camera.originZ);

            batch.addChunkRender(start, count, modelX, modelY, modelZ);
        }
    }

    private static int getUploadQueuePayloadSize(List<ChunkBuildResult<LCBGraphicsState>> queue) {
        int size = 0;

        for (ChunkBuildResult<LCBGraphicsState> result : queue) {
            size += result.data.getMeshData().getSize();
        }

        return size;
    }

    @Override
    public void delete() {
        super.delete();

        this.bufferManager.delete();
        this.uploadBuffer.delete();
    }

    public static boolean isSupported() {
        return GlFunctions.isVertexArraySupported() && GlFunctions.isBufferCopySupported() && GlFunctions.isShaderDrawParametersSupported();
    }
}
