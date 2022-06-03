package net.caffeinemc.sodium.render.chunk.draw;

import it.unimi.dsi.fastutil.objects.Reference2ReferenceArrayMap;

import java.util.*;

import net.caffeinemc.gfx.api.buffer.Buffer;
import net.caffeinemc.gfx.api.buffer.MappedBufferFlags;
import net.caffeinemc.gfx.api.device.RenderDevice;
import net.caffeinemc.sodium.SodiumClientMod;
import net.caffeinemc.sodium.render.buffer.StreamingBuffer;
import net.caffeinemc.sodium.render.chunk.passes.ChunkRenderPass;
import net.caffeinemc.sodium.render.chunk.passes.DefaultRenderPasses;
import net.caffeinemc.sodium.render.chunk.state.ChunkRenderBounds;
import net.caffeinemc.sodium.render.chunk.state.UploadedChunkGeometry.ModelPart;
import net.caffeinemc.sodium.render.terrain.quad.properties.ChunkMeshFace;
import net.caffeinemc.sodium.util.MathUtil;
import net.minecraft.util.math.ChunkSectionPos;

import java.nio.ByteBuffer;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

public class RenderListBuilder {
    private static final int COMMAND_STRUCT_STRIDE = 20;
    private static final int COMMAND_STRUCT_SIZE = Integer.BYTES * 5;
    private static final int COMMAND_STRUCT_PADDING = COMMAND_STRUCT_STRIDE - COMMAND_STRUCT_SIZE;

    private static final int INSTANCE_STRUCT_STRIDE = 16;
    private static final int INSTANCE_STRUCT_SIZE = Float.BYTES * 3;
    private static final int INSTANCE_STRUCT_PADDING = INSTANCE_STRUCT_STRIDE - INSTANCE_STRUCT_SIZE;

    private final StreamingBuffer commandBuffer;
    private final StreamingBuffer instanceBuffer;

    public RenderListBuilder(RenderDevice device) {
        var maxInFlightFrames = SodiumClientMod.options().advanced.cpuRenderAheadLimit + 1;
        var uboAlignment = device.properties().uniformBufferOffsetAlignment;

        this.commandBuffer = new StreamingBuffer(
                device,
                1,
                0x10000, // start with 64KiB per section and expand from there if needed (this is a complete guess lol)
                maxInFlightFrames,
                MappedBufferFlags.EXPLICIT_FLUSH
        );
        this.instanceBuffer = new StreamingBuffer(
                device,
                device.properties().uniformBufferOffsetAlignment,
                8 * 4 * 8 * uboAlignment, // worst case
                maxInFlightFrames,
                MappedBufferFlags.EXPLICIT_FLUSH
        );
    }

    public StreamingBuffer getInstanceBuffer() {
        return this.instanceBuffer;
    }

    public StreamingBuffer getCommandBuffer() {
        return this.commandBuffer;
    }

    public Map<ChunkRenderPass, RenderList> createRenderLists(SortedChunkLists chunks, ChunkCameraContext camera, int frameIndex) {
        if (chunks.isEmpty()) {
            return null;
        }

        var commandBufferSize = commandBufferSize(1, chunks);
        StreamingBuffer.WritableSection commandBufferSection = this.commandBuffer.getSection(frameIndex, (int) commandBufferSize, true);
        ByteBuffer commandBufferView = commandBufferSection.getView();

        var instanceBufferSize = instanceBufferSize(this.instanceBuffer.getAlignment(), chunks);
        StreamingBuffer.WritableSection instanceBufferSection = this.instanceBuffer.getSection(frameIndex, (int) instanceBufferSize, true);
        ByteBuffer instanceBufferView = instanceBufferSection.getView();

        var renderLists = new Reference2ReferenceArrayMap<ChunkRenderPass, RenderList>();

        // this may be able to go on the main stack, but i don't wanna risk it
        ByteBuffer stackBuffer = MemoryUtil.memAlloc((int) ((commandBufferSize + instanceBufferSize) * DefaultRenderPasses.ALL.length));
        try (MemoryStack stack = MemoryStack.create(stackBuffer).push()) {
            for (var pass : DefaultRenderPasses.ALL) {
                renderLists.put(
                        pass,
                        new RenderList(
                                stack.malloc(1, (int) commandBufferSize),
                                stack.malloc(1, (int) instanceBufferSize)
                        )
                );
            }

            var reverseOrder = false; // TODO: fix me

            for (var bucketIterator = chunks.sorted(reverseOrder); bucketIterator.hasNext(); ) {
                var bucket = bucketIterator.next();

                for (var sectionIterator = bucket.sorted(reverseOrder); sectionIterator.hasNext(); ) {
                    var section = sectionIterator.next();

                    var geometry = section.getGeometry();
                    var baseVertex = geometry.segment.getOffset();

                    var visibility = calculateVisibilityFlags(section.getBounds(), camera);

                    for (var model : geometry.models) {
                        if ((model.visibilityBits & visibility) == 0) {
                            continue;
                        }

                        var renderList = renderLists.get(model.pass);

                        if (renderList == null) {
                            continue;
                        }

                        for (var range : model.ranges) {
                            if ((visibility & range) == 0) {
                                continue;
                            }

                            var vertexCount = ModelPart.unpackVertexCount(range);
                            var firstVertex = baseVertex + ModelPart.unpackFirstVertex(range);

                            ByteBuffer tempCommandBuffer = renderList.tempCommandBuffer;
                            tempCommandBuffer
                                    .putInt(vertexCount)
                                    .putInt(1)
                                    .putInt(0)
                                    .putInt(firstVertex) // baseVertex
                                    .putInt(renderList.currentInstanceCount); // baseInstance
                            if (COMMAND_STRUCT_PADDING > 0) {
                                tempCommandBuffer.position(tempCommandBuffer.position() + COMMAND_STRUCT_PADDING);
                            }
                            renderList.currentCommandCount++;
                        }

                        float x = getCameraTranslation(ChunkSectionPos.getBlockCoord(section.getChunkX()), camera.blockX, camera.deltaX);
                        float y = getCameraTranslation(ChunkSectionPos.getBlockCoord(section.getChunkY()), camera.blockY, camera.deltaY);
                        float z = getCameraTranslation(ChunkSectionPos.getBlockCoord(section.getChunkZ()), camera.blockZ, camera.deltaZ);

                        ByteBuffer tempInstanceBuffer = renderList.tempInstanceBuffer;
                        tempInstanceBuffer
                                .putFloat(x)
                                .putFloat(y)
                                .putFloat(z);
                        if (INSTANCE_STRUCT_PADDING > 0) {
                            tempInstanceBuffer.position(tempInstanceBuffer.position() + INSTANCE_STRUCT_PADDING);
                        }
                        renderList.currentInstanceCount++;

                        renderList.largestVertexIndex = Math.max(renderList.largestVertexIndex, geometry.segment.getLength());
                    }
                }

                for (var renderList : renderLists.values()) {
                    var instanceCount = renderList.currentInstanceCount;
                    var commandCount = renderList.currentCommandCount;
                    renderList.currentCommandCount = 0;
                    renderList.currentInstanceCount = 0;

                    if (commandCount <= 0) {
                        continue;
                    }


                    ByteBuffer tempCommandBuffer = renderList.tempCommandBuffer;

                    var commandCurrentPosition = tempCommandBuffer.position();
                    var commandSectionLength = commandCount * COMMAND_STRUCT_STRIDE;
                    var commandSectionStart = commandCurrentPosition - commandSectionLength;

                    ByteBuffer tempInstanceBuffer = renderList.tempInstanceBuffer;

                    var instanceCurrentPosition = tempInstanceBuffer.position();
                    var instanceSectionLength = instanceCount * INSTANCE_STRUCT_STRIDE;
                    var instanceSectionStart = instanceCurrentPosition - instanceSectionLength;

                    // don't need to align command buffer data, only instance data
                    tempInstanceBuffer.position(MathUtil.align(instanceCurrentPosition, this.instanceBuffer.getAlignment()));

                    var region = bucket.region();

                    renderList.batches.add(
                            new ChunkRenderBatch(
                                    region.vertexBuffers.getBufferObject(),
                                    region.vertexBuffers.getStride(),
                                    instanceCount,
                                    commandCount,
                                    instanceSectionStart,
                                    commandSectionStart
                            )
                    );
                }
            }

            for (var renderListIterator = renderLists.values().iterator(); renderListIterator.hasNext(); ) {
                var renderList = renderListIterator.next();

                if (renderList.batches.size() <= 0) {
                    renderListIterator.remove();
                    continue;
                }

                int mainCommandBufferOffset = commandBufferView.position();
                int mainInstanceBufferOffset = instanceBufferView.position();
                commandBufferView.put(renderList.tempCommandBuffer.flip());
                instanceBufferView.put(renderList.tempInstanceBuffer.flip());

                for (var batch : renderList.batches) {
                    batch.commandBufferOffset += mainCommandBufferOffset;
                    batch.instanceBufferOffset += mainInstanceBufferOffset;
                }
            }
        }
        MemoryUtil.memFree(stackBuffer);

        commandBufferSection.flushPartial();
        instanceBufferSection.flushPartial();

        return renderLists;
    }

    public static class RenderList {
        private final Deque<ChunkRenderBatch> batches;
        private int largestVertexIndex;

        // these will be deallocated by the time construction is done
        private final ByteBuffer tempCommandBuffer;
        private final ByteBuffer tempInstanceBuffer;

        private int currentInstanceCount;
        private int currentCommandCount;

        public RenderList(ByteBuffer tempCommandBuffer, ByteBuffer tempInstanceBuffer) {
            this.tempCommandBuffer = tempCommandBuffer;
            this.tempInstanceBuffer = tempInstanceBuffer;
            this.batches = new ArrayDeque<>();
        }

        public Collection<ChunkRenderBatch> getBatches() {
            return this.batches;
        }

        public int getLargestVertexIndex() {
            return this.largestVertexIndex;
        }

    }

    public static final class ChunkRenderBatch {
        private final Buffer vertexBuffer;
        private final int vertexStride;
        private final int instanceCount;
        private final int commandCount;
        private int instanceBufferOffset;
        private int commandBufferOffset;

        public ChunkRenderBatch(Buffer vertexBuffer,
                                int vertexStride,
                                int instanceCount,
                                int commandCount,
                                int instanceBufferOffset,
                                int commandBufferOffset
        ) {
            this.vertexBuffer = vertexBuffer;
            this.vertexStride = vertexStride;
            this.instanceCount = instanceCount;
            this.commandCount = commandCount;
            this.instanceBufferOffset = instanceBufferOffset;
            this.commandBufferOffset = commandBufferOffset;
        }

        public Buffer getVertexBuffer() {
            return this.vertexBuffer;
        }

        public int getVertexStride() {
            return this.vertexStride;
        }

        public int getInstanceCount() {
            return this.instanceCount;
        }

        public int getCommandCount() {
            return this.commandCount;
        }

        public int getInstanceBufferOffset() {
            return this.instanceBufferOffset;
        }

        public int getCommandBufferOffset() {
            return this.commandBufferOffset;
        }
    }

    private static long commandBufferSize(int alignment, SortedChunkLists list) {
        int size = 0;

        for (var bucket : list.unsorted()) {
            size += MathUtil.align((bucket.size() * ChunkMeshFace.COUNT) * COMMAND_STRUCT_STRIDE, alignment);
        }

        return size;
    }

    private static long instanceBufferSize(int alignment, SortedChunkLists list) {
        int size = 0;

        for (var bucket : list.unsorted()) {
            size += MathUtil.align(bucket.size() * INSTANCE_STRUCT_STRIDE, alignment);
        }

        return size;
    }

    private static float getCameraTranslation(int chunkBlockPos, int cameraBlockPos, float cameraPos) {
        return (chunkBlockPos - cameraBlockPos) - cameraPos;
    }

    private static int calculateVisibilityFlags(ChunkRenderBounds bounds, ChunkCameraContext camera) {
        int flags = ChunkMeshFace.UNASSIGNED_BITS;

        if (camera.posY > bounds.y1) {
            flags |= ChunkMeshFace.UP_BITS;
        }

        if (camera.posY < bounds.y2) {
            flags |= ChunkMeshFace.DOWN_BITS;
        }

        if (camera.posX > bounds.x1) {
            flags |= ChunkMeshFace.EAST_BITS;
        }

        if (camera.posX < bounds.x2) {
            flags |= ChunkMeshFace.WEST_BITS;
        }

        if (camera.posZ > bounds.z1) {
            flags |= ChunkMeshFace.SOUTH_BITS;
        }

        if (camera.posZ < bounds.z2) {
            flags |= ChunkMeshFace.NORTH_BITS;
        }

        return flags;
    }

}
