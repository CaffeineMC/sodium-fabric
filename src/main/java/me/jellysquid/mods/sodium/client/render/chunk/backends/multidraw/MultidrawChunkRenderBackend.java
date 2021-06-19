package me.jellysquid.mods.sodium.client.render.chunk.backends.multidraw;

import it.unimi.dsi.fastutil.objects.ObjectArrayFIFOQueue;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import me.jellysquid.mods.sodium.client.gl.arena.GlBufferArena;
import me.jellysquid.mods.sodium.client.gl.arena.GlBufferSegment;
import me.jellysquid.mods.sodium.client.gl.attribute.GlVertexAttribute;
import me.jellysquid.mods.sodium.client.gl.attribute.GlVertexAttributeBinding;
import me.jellysquid.mods.sodium.client.gl.attribute.GlVertexAttributeFormat;
import me.jellysquid.mods.sodium.client.gl.buffer.*;
import me.jellysquid.mods.sodium.client.gl.device.DrawCommandList;
import me.jellysquid.mods.sodium.client.gl.func.GlFunctions;
import me.jellysquid.mods.sodium.client.gl.device.CommandList;
import me.jellysquid.mods.sodium.client.gl.device.RenderDevice;
import me.jellysquid.mods.sodium.client.gl.tessellation.GlPrimitiveType;
import me.jellysquid.mods.sodium.client.gl.tessellation.GlTessellation;
import me.jellysquid.mods.sodium.client.gl.tessellation.TessellationBinding;
import me.jellysquid.mods.sodium.client.gl.util.BufferSlice;
import me.jellysquid.mods.sodium.client.model.quad.properties.ModelQuadFacing;
import me.jellysquid.mods.sodium.client.model.vertex.type.ChunkVertexType;
import me.jellysquid.mods.sodium.client.render.chunk.ChunkCameraContext;
import me.jellysquid.mods.sodium.client.render.chunk.ChunkRenderContainer;
import me.jellysquid.mods.sodium.client.render.chunk.compile.ChunkBuildResult;
import me.jellysquid.mods.sodium.client.render.chunk.data.ChunkMeshData;
import me.jellysquid.mods.sodium.client.render.chunk.data.ChunkRenderData;
import me.jellysquid.mods.sodium.client.render.chunk.format.ChunkMeshAttribute;
import me.jellysquid.mods.sodium.client.render.chunk.lists.ChunkRenderListIterator;
import me.jellysquid.mods.sodium.client.render.chunk.passes.BlockRenderPass;
import me.jellysquid.mods.sodium.client.render.chunk.region.ChunkRegion;
import me.jellysquid.mods.sodium.client.render.chunk.region.ChunkRegionManager;
import me.jellysquid.mods.sodium.client.render.chunk.shader.ChunkRenderShaderBackend;
import me.jellysquid.mods.sodium.client.render.chunk.shader.ChunkShaderBindingPoints;
import net.minecraft.util.Formatting;
import net.minecraft.util.Util;
import org.lwjgl.opengl.GL20C;

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
public class MultidrawChunkRenderBackend extends ChunkRenderShaderBackend<MultidrawGraphicsState> {
    private final ChunkRegionManager<MultidrawGraphicsState> bufferManager;

    private final ObjectArrayList<ChunkRegion<MultidrawGraphicsState>> pendingBatches = new ObjectArrayList<>();
    private final ObjectArrayFIFOQueue<ChunkRegion<MultidrawGraphicsState>> pendingUploads = new ObjectArrayFIFOQueue<>();

    private final GlMutableBuffer uploadBuffer;
    private final GlMutableBuffer uniformBuffer;
    private final GlMutableBuffer commandBuffer;

    private final ChunkDrawParamsVector uniformBufferBuilder;
    private final IndirectCommandBufferVector commandClientBufferBuilder;

    public MultidrawChunkRenderBackend(RenderDevice device, ChunkVertexType vertexType) {
        super(vertexType);

        this.bufferManager = new ChunkRegionManager<>(device);

        try (CommandList commands = device.createCommandList()) {
            this.uploadBuffer = commands.createMutableBuffer(GlBufferUsage.GL_STREAM_DRAW);
            this.uniformBuffer = commands.createMutableBuffer(GlBufferUsage.GL_STATIC_DRAW);
            this.commandBuffer = isWindowsIntelDriver() ? null : commands.createMutableBuffer(GlBufferUsage.GL_STREAM_DRAW);
        }

        this.uniformBufferBuilder = ChunkDrawParamsVector.create(2048);
        this.commandClientBufferBuilder = IndirectCommandBufferVector.create(2048);
    }

    @Override
    public void upload(CommandList commandList, Iterator<ChunkBuildResult<MultidrawGraphicsState>> queue) {
        this.setupUploadBatches(queue);

        commandList.bindBuffer(GlBufferTarget.ARRAY_BUFFER, this.uploadBuffer);

        while (!this.pendingUploads.isEmpty()) {
            ChunkRegion<MultidrawGraphicsState> region = this.pendingUploads.dequeue();

            GlBufferArena arena = region.getBufferArena();
            GlBuffer buffer = arena.getBuffer();

            ObjectArrayList<ChunkBuildResult<MultidrawGraphicsState>> uploadQueue = region.getUploadQueue();
            arena.prepareBuffer(commandList, getUploadQueuePayloadSize(uploadQueue));

            for (ChunkBuildResult<MultidrawGraphicsState> result : uploadQueue) {
                ChunkRenderContainer<MultidrawGraphicsState> render = result.render;
                ChunkRenderData data = result.data;

                for (BlockRenderPass pass : BlockRenderPass.VALUES) {
                    MultidrawGraphicsState graphics = render.getGraphicsState(pass);

                    // De-allocate the existing buffer arena for this render
                    // This will allow it to be cheaply re-allocated just below
                    if (graphics != null) {
                        graphics.delete(commandList);
                    }

                    ChunkMeshData meshData = data.getMesh(pass);

                    if (meshData.hasVertexData()) {
                        VertexData upload = meshData.takeVertexData();

                        commandList.uploadData(this.uploadBuffer, upload.buffer);

                        GlBufferSegment segment = arena.uploadBuffer(commandList, this.uploadBuffer, 0, upload.buffer.capacity());

                        render.setGraphicsState(pass, new MultidrawGraphicsState(render, region, segment, meshData, this.vertexFormat));
                    } else {
                        render.setGraphicsState(pass, null);
                    }
                }

                render.setData(data);
            }

            // Check if the tessellation needs to be updated
            // This happens whenever the backing buffer object for the arena changes, or if it hasn't already been created
            if (region.getTessellation() == null || buffer != arena.getBuffer()) {
                if (region.getTessellation() != null) {
                    commandList.deleteTessellation(region.getTessellation());
                }

                region.setTessellation(this.createRegionTessellation(commandList, arena.getBuffer()));
            }

            uploadQueue.clear();
        }

        commandList.invalidateBuffer(this.uploadBuffer);
    }

    private GlTessellation createRegionTessellation(CommandList commandList, GlBuffer buffer) {
        return commandList.createTessellation(GlPrimitiveType.QUADS, new TessellationBinding[] {
                new TessellationBinding(buffer, new GlVertexAttributeBinding[] {
                        new GlVertexAttributeBinding(ChunkShaderBindingPoints.POSITION,
                                this.vertexFormat.getAttribute(ChunkMeshAttribute.POSITION)),
                        new GlVertexAttributeBinding(ChunkShaderBindingPoints.COLOR,
                                this.vertexFormat.getAttribute(ChunkMeshAttribute.COLOR)),
                        new GlVertexAttributeBinding(ChunkShaderBindingPoints.TEX_COORD,
                                this.vertexFormat.getAttribute(ChunkMeshAttribute.TEXTURE)),
                        new GlVertexAttributeBinding(ChunkShaderBindingPoints.LIGHT_COORD,
                                this.vertexFormat.getAttribute(ChunkMeshAttribute.LIGHT))
                }, false),
                new TessellationBinding(this.uniformBuffer, new GlVertexAttributeBinding[] {
                        new GlVertexAttributeBinding(ChunkShaderBindingPoints.MODEL_OFFSET,
                                new GlVertexAttribute(GlVertexAttributeFormat.FLOAT, 4, false, 0, 0))
                }, true)
        });
    }

    @Override
    public void render(CommandList commandList, ChunkRenderListIterator<MultidrawGraphicsState> renders, ChunkCameraContext camera) {
        this.bufferManager.cleanup();

        this.setupDrawBatches(commandList, renders, camera);
        this.buildCommandBuffer();

        if (this.commandBuffer != null) {
            commandList.bindBuffer(GlBufferTarget.DRAW_INDIRECT_BUFFER, this.commandBuffer);
            commandList.uploadData(this.commandBuffer, this.commandClientBufferBuilder.getBuffer());
        }

        long pointer = this.commandBuffer == null ? this.commandClientBufferBuilder.getBufferAddress() : 0L;

        for (ChunkRegion<?> region : this.pendingBatches) {
            ChunkDrawCallBatcher batch = region.getDrawBatcher();

            try (DrawCommandList drawCommandList = commandList.beginTessellating(region.getTessellation())) {
                drawCommandList.multiDrawArraysIndirect(pointer, batch.getCount(), 0 /* tightly packed */);
            }

            pointer += batch.getArrayLength();
        }

        this.pendingBatches.clear();
    }

    private void buildCommandBuffer() {
        this.commandClientBufferBuilder.begin();

        for (ChunkRegion<?> region : this.pendingBatches) {
            ChunkDrawCallBatcher batcher = region.getDrawBatcher();
            batcher.end();

            this.commandClientBufferBuilder.pushCommandBuffer(batcher);
        }

        this.commandClientBufferBuilder.end();
    }

    private void setupUploadBatches(Iterator<ChunkBuildResult<MultidrawGraphicsState>> renders) {
        while (renders.hasNext()) {
            ChunkBuildResult<MultidrawGraphicsState> result = renders.next();
            ChunkRenderContainer<MultidrawGraphicsState> render = result.render;

            ChunkRegion<MultidrawGraphicsState> region = this.bufferManager.getRegion(render.getChunkX(), render.getChunkY(), render.getChunkZ());

            if (region == null) {
                if (result.data.getMeshSize() <= 0) {
                    render.setData(result.data);
                    continue;
                }

                region = this.bufferManager.getOrCreateRegion(render.getChunkX(), render.getChunkY(), render.getChunkZ());
            }

            ObjectArrayList<ChunkBuildResult<MultidrawGraphicsState>> uploadQueue = region.getUploadQueue();

            if (uploadQueue.isEmpty()) {
                this.pendingUploads.enqueue(region);
            }

            uploadQueue.add(result);
        }
    }

    private void setupDrawBatches(CommandList commandList, ChunkRenderListIterator<MultidrawGraphicsState> it, ChunkCameraContext camera) {
        this.uniformBufferBuilder.reset();

        int drawCount = 0;

        while (it.hasNext()) {
            MultidrawGraphicsState state = it.getGraphicsState();
            int visible = it.getVisibleFaces();

            int index = drawCount++;
            float x = camera.getChunkModelOffset(state.getX(), camera.blockOriginX, camera.originX);
            float y = camera.getChunkModelOffset(state.getY(), camera.blockOriginY, camera.originY);
            float z = camera.getChunkModelOffset(state.getZ(), camera.blockOriginZ, camera.originZ);

            this.uniformBufferBuilder.pushChunkDrawParams(x, y, z);

            ChunkRegion<MultidrawGraphicsState> region = state.getRegion();
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

        commandList.uploadData(this.uniformBuffer, this.uniformBufferBuilder.getBuffer());
    }

    private static int getUploadQueuePayloadSize(List<ChunkBuildResult<MultidrawGraphicsState>> queue) {
        int size = 0;

        for (ChunkBuildResult<MultidrawGraphicsState> result : queue) {
            size += result.data.getMeshSize();
        }

        return size;
    }

    @Override
    public void delete() {
        super.delete();

        try (CommandList commands = RenderDevice.INSTANCE.createCommandList()) {
            commands.deleteBuffer(this.uploadBuffer);
            commands.deleteBuffer(this.uniformBuffer);

            if (this.commandBuffer != null) {
                commands.deleteBuffer(this.commandBuffer);
            }
        }

        this.bufferManager.delete();

        this.commandClientBufferBuilder.delete();
        this.uniformBufferBuilder.delete();
    }

    @Override
    public Class<MultidrawGraphicsState> getGraphicsStateType() {
        return MultidrawGraphicsState.class;
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
        return Objects.equals(GL20C.glGetString(GL20C.GL_VENDOR), INTEL_VENDOR_NAME);
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

        String version = GL20C.glGetString(GL20C.GL_VERSION);

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
        return "Multidraw";
    }

    @Override
    public List<String> getDebugStrings() {
        List<String> list = new ArrayList<>();
        list.add(String.format("Active Buffers: %s", this.bufferManager.getAllocatedRegionCount()));
        list.add(String.format("Submission Mode: %s", this.commandBuffer != null ?
                Formatting.AQUA + "Buffer" : Formatting.LIGHT_PURPLE + "Client Memory"));

        return list;
    }
}
