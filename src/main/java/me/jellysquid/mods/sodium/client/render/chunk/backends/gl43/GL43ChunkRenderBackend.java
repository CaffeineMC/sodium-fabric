package me.jellysquid.mods.sodium.client.render.chunk.backends.gl43;

import it.unimi.dsi.fastutil.objects.ObjectArrayFIFOQueue;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import me.jellysquid.mods.sodium.client.gl.arena.GlBufferArena;
import me.jellysquid.mods.sodium.client.gl.arena.GlBufferRegion;
import me.jellysquid.mods.sodium.client.gl.array.GlVertexArray;
import me.jellysquid.mods.sodium.client.gl.buffer.GlBuffer;
import me.jellysquid.mods.sodium.client.gl.buffer.GlMutableBuffer;
import me.jellysquid.mods.sodium.client.gl.buffer.VertexData;
import me.jellysquid.mods.sodium.client.gl.func.GlFunctions;
import me.jellysquid.mods.sodium.client.gl.util.BufferSlice;
import me.jellysquid.mods.sodium.client.gl.util.MemoryTracker;
import me.jellysquid.mods.sodium.client.model.quad.properties.ModelQuadFacing;
import me.jellysquid.mods.sodium.client.model.vertex.type.ChunkVertexType;
import me.jellysquid.mods.sodium.client.render.chunk.ChunkCameraContext;
import me.jellysquid.mods.sodium.client.render.chunk.ChunkRenderContainer;
import me.jellysquid.mods.sodium.client.render.chunk.compile.ChunkBuildResult;
import me.jellysquid.mods.sodium.client.render.chunk.data.ChunkMeshData;
import me.jellysquid.mods.sodium.client.render.chunk.data.ChunkRenderData;
import me.jellysquid.mods.sodium.client.render.chunk.lists.ChunkRenderListIterator;
import me.jellysquid.mods.sodium.client.render.chunk.multidraw.ChunkDrawCallBatcher;
import me.jellysquid.mods.sodium.client.render.chunk.multidraw.ChunkDrawParamsVector;
import me.jellysquid.mods.sodium.client.render.chunk.multidraw.ChunkRenderBackendMultiDraw;
import me.jellysquid.mods.sodium.client.render.chunk.passes.BlockRenderPass;
import me.jellysquid.mods.sodium.client.render.chunk.region.ChunkRegion;
import me.jellysquid.mods.sodium.client.render.chunk.region.ChunkRegionManager;
import net.minecraft.util.Formatting;
import net.minecraft.util.Util;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL40;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
 * With both of these changes, the amount of CPU time taken by rendering chunks linearly decreases with the reduction
 * in buffer bind/setup/draw calls. Using the default settings of 4x2x4 chunk region buffers, the number of calls can be
 * reduced up to a factor of ~32x.
 */
public class GL43ChunkRenderBackend extends ChunkRenderBackendMultiDraw<LCBGraphicsState> {
    private final ChunkRegionManager bufferManager;

    private final ObjectArrayList<ChunkRegion> pendingBatches = new ObjectArrayList<>();
    private final ObjectArrayFIFOQueue<ChunkRegion> pendingUploads = new ObjectArrayFIFOQueue<>();

    private final GlMutableBuffer uploadBuffer;
    private final GlMutableBuffer uniformBuffer;
    private final GlMutableBuffer commandBuffer;

    private final ChunkDrawParamsVector uniformBufferBuilder;
    private final IndirectCommandBufferVector commandClientBufferBuilder;

    private final MemoryTracker memoryTracker = new MemoryTracker();

    public GL43ChunkRenderBackend(ChunkVertexType vertexType) {
        super(LCBGraphicsState.class, vertexType);

        this.bufferManager = new ChunkRegionManager(this.memoryTracker);
        this.uploadBuffer = new GlMutableBuffer(GL15.GL_STREAM_COPY);

        this.uniformBuffer = new GlMutableBuffer(GL15.GL_STATIC_DRAW);
        this.uniformBufferBuilder = ChunkDrawParamsVector.create(2048);

        this.commandBuffer = isWindowsIntelDriver() ? null : new GlMutableBuffer(GL15.GL_STATIC_DRAW);
        this.commandClientBufferBuilder = IndirectCommandBufferVector.create(2048);
    }

    @Override
    public void upload(Iterator<ChunkBuildResult> queue) {
        this.setupUploadBatches(queue);

        GlMutableBuffer uploadBuffer = this.uploadBuffer;
        uploadBuffer.bind(GL15.GL_ARRAY_BUFFER);

        while (!this.pendingUploads.isEmpty()) {
            ChunkRegion region = this.pendingUploads.dequeue();

            GlBufferArena arena = region.getBufferArena();
            arena.bind();

            ObjectArrayList<ChunkBuildResult> uploadQueue = region.getUploadQueue();
            arena.ensureCapacity(getUploadQueuePayloadSize(uploadQueue));

            for (ChunkBuildResult result : uploadQueue) {
                ChunkRenderContainer render = result.render;
                ChunkRenderData data = result.data;

                for (BlockRenderPass pass : BlockRenderPass.VALUES) {
                    int graphicsId = render.getGraphicsStates().get(pass);

                    // De-allocate the existing buffer arena for this render
                    // This will allow it to be cheaply re-allocated just below
                    if (graphicsId != -1) {
                        this.stateStorage.remove(graphicsId).delete();
                        this.stateIds.deallocateId(graphicsId);
                    }

                    ChunkMeshData meshData = data.getMesh(pass);

                    if (meshData.hasVertexData()) {
                        VertexData upload = meshData.takeVertexData();
                        uploadBuffer.upload(GL15.GL_ARRAY_BUFFER, upload);

                        GlBufferRegion segment = arena.upload(GL15.GL_ARRAY_BUFFER, 0, upload.buffer.capacity());

                        LCBGraphicsState state = new LCBGraphicsState(render, region, segment, meshData, this.vertexFormat, this.stateIds.allocateId());
                        this.stateStorage.add(state);

                        render.getGraphicsStates().set(pass, state.getId());
                    } else {
                        render.getGraphicsStates().remove(pass);
                    }
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
    public void render(ChunkRenderListIterator renders, ChunkCameraContext camera) {
        this.bufferManager.cleanup();

        this.setupDrawBatches(renders, camera);
        this.buildCommandBuffer();

        if (this.commandBuffer != null) {
            this.commandBuffer.bind(GL40.GL_DRAW_INDIRECT_BUFFER);
            this.commandBuffer.upload(GL40.GL_DRAW_INDIRECT_BUFFER, this.commandClientBufferBuilder.getBuffer());
        }

        GlVertexArray prevVao = null;

        long commandStart = this.commandBuffer == null ? this.commandClientBufferBuilder.getBufferAddress() : 0L;

        for (ChunkRegion region : this.pendingBatches) {
            GlVertexArray vao = region.getVertexArray();
            vao.bind();

            // Check if the VAO's bindings need to be updated
            // This happens whenever the backing buffer object for the arena changes
            if (region.isDirty()) {
                this.setupArrayBufferState(region.getBufferArena());
                this.setupUniformBufferState();

                region.markClean();
            }

            ChunkDrawCallBatcher batch = region.getDrawBatcher();

            GlFunctions.INDIRECT_DRAW.glMultiDrawArraysIndirect(GL11.GL_QUADS, commandStart, batch.getCount(), 0 /* tightly packed */);
            commandStart += batch.getArrayLength();

            prevVao = vao;
        }

        this.pendingBatches.clear();

        if (prevVao != null) {
            prevVao.unbind();
        }

        this.uniformBuffer.unbind(GL15.GL_ARRAY_BUFFER);

        if (this.commandBuffer != null) {
            this.commandBuffer.unbind(GL40.GL_DRAW_INDIRECT_BUFFER);
        }
    }

    private void buildCommandBuffer() {
        this.commandClientBufferBuilder.begin();

        for (ChunkRegion region : this.pendingBatches) {
            ChunkDrawCallBatcher batcher = region.getDrawBatcher();
            batcher.end();

            this.commandClientBufferBuilder.pushCommandBuffer(batcher);
        }

        this.commandClientBufferBuilder.end();
    }

    private void setupArrayBufferState(GlBufferArena arena) {
        GlBuffer vbo = arena.getBuffer();
        vbo.bind(GL15.GL_ARRAY_BUFFER);

        this.vertexFormat.bindVertexAttributes();
        this.vertexFormat.enableVertexAttributes();
    }

    private void setupUniformBufferState() {
        this.uniformBuffer.bind(GL15.GL_ARRAY_BUFFER);

        int index = this.activeProgram.getModelOffsetAttributeLocation();

        // Bind a packed array buffer containing model transformations for each chunk. This provides an alternative to
        // gl_DrawID in OpenGL 4.6 and should work on more hardware.
        //
        // The base instance value assigned to each indirect draw call decides the starting offset of vertices in a
        // "instanced" vertex attributes. By specifying a divisor of 1, an instanced vertex attribute will always point
        // to the starting element (the base instance) in the array buffer, thereby acting as a per-draw constant.
        //
        // This provides performance as good as a uniform array without the need to split draw call batches due to
        // uniform array size limits. All uniforms can be uploaded and bound in a single call.
        GL20.glVertexAttribPointer(index, 4, GL11.GL_FLOAT, false, 0, 0L);
        GlFunctions.INSTANCED_ARRAY.glVertexAttribDivisor(index, 1);

        GL20.glEnableVertexAttribArray(index);
    }

    private void setupUploadBatches(Iterator<ChunkBuildResult> renders) {
        while (renders.hasNext()) {
            ChunkBuildResult result = renders.next();
            ChunkRenderContainer render = result.render;

            ChunkRegion region = this.bufferManager.getRegion(render.getChunkX(), render.getChunkY(), render.getChunkZ());

            if (region == null) {
                if (result.data.getMeshSize() <= 0) {
                    render.setData(result.data);
                    continue;
                }

                region = this.bufferManager.getOrCreateRegion(render.getChunkX(), render.getChunkY(), render.getChunkZ());
            }

            ObjectArrayList<ChunkBuildResult> uploadQueue = region.getUploadQueue();

            if (uploadQueue.isEmpty()) {
                this.pendingUploads.enqueue(region);
            }

            uploadQueue.add(result);
        }
    }

    private void setupDrawBatches(ChunkRenderListIterator it, ChunkCameraContext camera) {
        this.uniformBufferBuilder.reset();

        int drawCount = 0;

        while (it.hasNext()) {
            LCBGraphicsState state = this.stateStorage.get(it.getGraphicsStateId());

            int visible = it.getVisibleFaces();

            int index = drawCount++;

            float x = camera.getChunkModelOffset(state.getX(), camera.blockOriginX, camera.originX);
            float y = camera.getChunkModelOffset(state.getY(), camera.blockOriginY, camera.originY);
            float z = camera.getChunkModelOffset(state.getZ(), camera.blockOriginZ, camera.originZ);

            this.uniformBufferBuilder.pushChunkDrawParams(x, y, z);

            ChunkRegion region = state.getRegion();
            ChunkDrawCallBatcher batch = region.getDrawBatcher();

            if (!batch.isBuilding()) {
                batch.begin();

                this.pendingBatches.add(region);
            }

            int mask = 0b1;

            for (int i = 0; i < ModelQuadFacing.COUNT; i++) {
                if ((visible & mask) != 0) {
                    long part = state.getModelPart(i);

                    batch.addIndirectDrawCall(BufferSlice.unpackStart(part), BufferSlice.unpackLength(part), index, 1);
                }

                mask <<= 1;
            }

            it.advance();
        }

        this.uniformBuffer.bind(GL15.GL_ARRAY_BUFFER);
        this.uniformBuffer.upload(GL15.GL_ARRAY_BUFFER, this.uniformBufferBuilder.getBuffer());
    }

    private static int getUploadQueuePayloadSize(List<ChunkBuildResult> queue) {
        int size = 0;

        for (ChunkBuildResult result : queue) {
            size += result.data.getMeshSize();
        }

        return size;
    }

    @Override
    public void delete() {
        super.delete();

        this.bufferManager.delete();
        this.uploadBuffer.delete();

        if (this.commandBuffer != null) {
            this.commandBuffer.delete();
        }

        this.commandClientBufferBuilder.delete();

        this.uniformBuffer.delete();
        this.uniformBufferBuilder.delete();
    }

    public static boolean isSupported(boolean disableDriverBlacklist) {
        if (!disableDriverBlacklist && isKnownBrokenIntelDriver()) {
            return false;
        }

        return GlFunctions.isVertexArraySupported() &&
                GlFunctions.isBufferCopySupported() &&
                GlFunctions.isIndirectMultiDrawSupported() &&
                GlFunctions.isInstancedArraySupported();
    }

    // https://www.intel.com/content/www/us/en/support/articles/000005654/graphics.html
    private static final Pattern INTEL_BUILD_MATCHER = Pattern.compile("(\\d.\\d.\\d) - Build (\\d+).(\\d+).(\\d+).(\\d+)");

    private static final String INTEL_VENDOR_NAME = "Intel";

    /**
     * Determines whether or not the current OpenGL renderer is an integrated Intel GPU on Windows.
     * These drivers on Windows are known to fail when using command buffers.
     */
    private static boolean isWindowsIntelDriver() {
        // We only care about Windows
        // The open-source drivers on Linux are not known to have driver bugs with indirect command buffers
        if (Util.getOperatingSystem() != Util.OperatingSystem.WINDOWS) {
            return false;
        }

        // Check to see if the GPU vendor is Intel
        return Objects.equals(GL11.glGetString(GL11.GL_VENDOR), INTEL_VENDOR_NAME);
    }

    /**
     * Determines whether or not the current OpenGL renderer is an old integrated Intel GPU (prior to Skylake/Gen8) on
     * Windows. These drivers on Windows are unsupported and known to create significant trouble with the multi-draw
     * renderer.
     */
    private static boolean isKnownBrokenIntelDriver() {
        if (!isWindowsIntelDriver()) {
            return false;
        }

        String version = GL11.glGetString(GL11.GL_VERSION);

        // The returned version string may be null in the case of an error
        if (version == null) {
            return false;
        }

        Matcher matcher = INTEL_BUILD_MATCHER.matcher(version);

        // If the version pattern doesn't match, assume we're dealing with something special
        if (!matcher.matches()) {
            return false;
        }

        // Anything with a major build of >=100 is GPU Gen8 or newer
        // The fourth group is the major build number
        return Integer.parseInt(matcher.group(4)) < 100;
    }

    @Override
    public String getRendererName() {
        return "Multidraw (GL 4.3)";
    }

    @Override
    public List<String> getDebugStrings() {
        // Allocated/Used in bytes
        long allocated = this.memoryTracker.getAllocatedMemory();
        long used = this.memoryTracker.getUsedMemory();

        int ratio = (int) Math.floor(((double) used / (double) allocated) * 100.0D);

        List<String> list = new ArrayList<>();
        list.add(String.format("VRAM Pool: %d/%d MiB %s(%d%%)%s", MemoryTracker.toMiB(used), MemoryTracker.toMiB(allocated),
                ratio >= 95 ? Formatting.RED : Formatting.RESET, ratio, Formatting.RESET));
        list.add(String.format("VRAM Regions: %s", this.bufferManager.getAllocatedRegionCount()));
        list.add(String.format("Submission Mode: %s", this.commandBuffer != null ?
                Formatting.AQUA + "Buffer" : Formatting.LIGHT_PURPLE + "Client Memory"));

        return list;
    }

    @Override
    public void deleteGraphicsState(int id) {
        this.stateStorage.remove(id)
                .delete();
    }
}
