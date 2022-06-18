package net.caffeinemc.sodium.render.chunk.draw;

import it.unimi.dsi.fastutil.objects.Reference2ReferenceArrayMap;
import net.caffeinemc.gfx.api.buffer.Buffer;
import net.caffeinemc.gfx.api.buffer.MappedBufferFlags;
import net.caffeinemc.gfx.api.device.RenderDevice;
import net.caffeinemc.sodium.SodiumClientMod;
import net.caffeinemc.sodium.render.buffer.streaming.DualStreamingBuffer;
import net.caffeinemc.sodium.render.buffer.streaming.SectionedStreamingBuffer;
import net.caffeinemc.sodium.render.buffer.streaming.StreamingBuffer;
import net.caffeinemc.sodium.render.chunk.passes.ChunkRenderPass;
import net.caffeinemc.sodium.render.chunk.passes.DefaultRenderPasses;
import net.caffeinemc.sodium.render.chunk.state.ChunkRenderBounds;
import net.caffeinemc.sodium.render.chunk.state.UploadedChunkGeometry.ModelPart;
import net.caffeinemc.sodium.render.terrain.quad.properties.ChunkMeshFace;
import net.caffeinemc.sodium.util.MathUtil;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.util.*;

public class RenderListBuilder {
    public static final int COMMAND_STRUCT_STRIDE = 5 * Integer.BYTES;
    public static final int INSTANCE_STRUCT_STRIDE = 4 * Float.BYTES;

    private final StreamingBuffer commandBuffer;
    private final StreamingBuffer instanceBuffer;

    private ByteBuffer stackBuffer;

    public RenderListBuilder(RenderDevice device, int chunkViewDistance, ClientWorld world) {
        var maxInFlightFrames = SodiumClientMod.options().advanced.cpuRenderAheadLimit + 1;
        var uboAlignment = device.properties().uniformBufferOffsetAlignment;

//        var maxSections = MathHelper.square((chunkViewDistance * 2) + 1) * world.countVerticalSections();
//        var maxRenderPasses = DefaultRenderPasses.ALL.length;
//
//        var maxInstanceBufferSize = maxSections * maxRenderPasses * Math.max(INSTANCE_STRUCT_STRIDE, uboAlignment);
//
//        var share3CoordsSections = 1; // the section the player is in, all faces possible
//        var share2CoordsSections = (chunkViewDistance * 4) + (world.countVerticalSections() - 1); // cardinal direction vectors from the player section, 5+1 faces possible
//        var share1CoordsSections = (MathHelper.square(chunkViewDistance) * 4) + (chunkViewDistance * (world.countVerticalSections() - 1) * 2); // cardinal planes intersecting the player section, 4+1 faces possible
//        var share0CoordsSections = maxSections - share3CoordsSections - share2CoordsSections - share1CoordsSections; // all other sections, 3+1 faces possible
//        var maxFaces = share3CoordsSections * ChunkMeshFace.COUNT
//                + share2CoordsSections * (ChunkMeshFace.COUNT - 1)
//                + share1CoordsSections * (ChunkMeshFace.COUNT - 2)
//                + share0CoordsSections * (ChunkMeshFace.COUNT - 3);
//        var maxCommandBufferSize = maxFaces * maxRenderPasses * COMMAND_STRUCT_STRIDE;

        this.commandBuffer = new DualStreamingBuffer(
                device,
                1,
                1048576, // start with 1 MiB and expand from there if needed
                maxInFlightFrames,
                EnumSet.of(
                        MappedBufferFlags.EXPLICIT_FLUSH,
                        MappedBufferFlags.CLIENT_STORAGE
                )
        );
        this.instanceBuffer = new SectionedStreamingBuffer(
                device,
                uboAlignment,
                1048576, // start with 1 MiB and expand from there if needed
                maxInFlightFrames,
                EnumSet.of(MappedBufferFlags.EXPLICIT_FLUSH)
        );

    }

    public StreamingBuffer getInstanceBuffer() {
        return this.instanceBuffer;
    }

    public StreamingBuffer getCommandBuffer() {
        return this.commandBuffer;
    }

    public int getDeviceBufferObjects() {
        return 3;
    }

    public long getDeviceUsedMemory() {
        return this.instanceBuffer.getDeviceUsedMemory()
                + this.commandBuffer.getDeviceUsedMemory();
    }

    public long getDeviceAllocatedMemory() {
        return this.instanceBuffer.getDeviceAllocatedMemory()
                + this.commandBuffer.getDeviceAllocatedMemory();
    }

    public Map<ChunkRenderPass, RenderList> createRenderLists(SortedChunkLists chunks, ChunkCameraContext camera, int frameIndex) {
        if (chunks.isEmpty()) {
            return null;
        }

        final int totalPasses = DefaultRenderPasses.ALL.length;

        var commandBufferPassSize = commandBufferPassSize(this.commandBuffer.getAlignment(), chunks);
        StreamingBuffer.WritableSection commandBufferSection = this.commandBuffer.getSection(frameIndex, commandBufferPassSize * totalPasses, false);
        ByteBuffer commandBufferSectionView = commandBufferSection.getView();
        long commandBufferSectionAddress = MemoryUtil.memAddress0(commandBufferSectionView);

        var instanceBufferPassSize = instanceBufferPassSize(this.instanceBuffer.getAlignment(), chunks);
        StreamingBuffer.WritableSection instanceBufferSection = this.instanceBuffer.getSection(frameIndex, instanceBufferPassSize * totalPasses, false);
        ByteBuffer instanceBufferSectionView = instanceBufferSection.getView();
        long instanceBufferSectionAddress = MemoryUtil.memAddress0(instanceBufferSectionView);

        var renderLists = new Reference2ReferenceArrayMap<ChunkRenderPass, RenderList>();

        int stackSize = (instanceBufferPassSize + commandBufferPassSize) * totalPasses;
        if (this.stackBuffer == null || this.stackBuffer.capacity() < stackSize) {
            MemoryUtil.memFree(this.stackBuffer); // this does nothing if the buffer is null
            this.stackBuffer = MemoryUtil.memAlloc(stackSize);
        }

        try (MemoryStack stack = MemoryStack.create(this.stackBuffer).push()) {
            for (var pass : DefaultRenderPasses.ALL) {
                renderLists.put(
                        pass,
                        new RenderList(
                                stack.nmalloc(1, commandBufferPassSize),
                                stack.nmalloc(1, instanceBufferPassSize)
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

                        if (renderList == null) { // very bad
                            continue;
                        }

                        for (var range : model.ranges) {
                            if ((visibility & range) == 0) {
                                continue;
                            }

                            var vertexCount = ModelPart.unpackVertexCount(range);
                            var firstVertex = baseVertex + ModelPart.unpackFirstVertex(range);

                            long ptr = renderList.tempCommandBufferAddress + renderList.tempCommandBufferPosition;
                            MemoryUtil.memPutInt(ptr + 0, vertexCount);
                            MemoryUtil.memPutInt(ptr + 4, 1);
                            MemoryUtil.memPutInt(ptr + 8, 0);
                            MemoryUtil.memPutInt(ptr + 12, firstVertex); // baseVertex
                            MemoryUtil.memPutInt(ptr + 16, renderList.currentInstanceCount); // baseInstance
                            renderList.tempCommandBufferPosition += COMMAND_STRUCT_STRIDE;
                            renderList.currentCommandCount++;
                        }

                        float x = getCameraTranslation(ChunkSectionPos.getBlockCoord(section.getChunkX()), camera.blockX, camera.deltaX);
                        float y = getCameraTranslation(ChunkSectionPos.getBlockCoord(section.getChunkY()), camera.blockY, camera.deltaY);
                        float z = getCameraTranslation(ChunkSectionPos.getBlockCoord(section.getChunkZ()), camera.blockZ, camera.deltaZ);

                        long ptr = renderList.tempInstanceBufferAddress + renderList.tempInstanceBufferPosition;
                        MemoryUtil.memPutFloat(ptr + 0, x);
                        MemoryUtil.memPutFloat(ptr + 4, y);
                        MemoryUtil.memPutFloat(ptr + 8, z);
                        renderList.tempInstanceBufferPosition += INSTANCE_STRUCT_STRIDE;
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

                    var commandCurrentPosition = renderList.tempCommandBufferPosition;
                    var commandSectionLength = commandCount * COMMAND_STRUCT_STRIDE;
                    var commandSectionStart = commandCurrentPosition - commandSectionLength;

                    var instanceCurrentPosition = renderList.tempInstanceBufferPosition;
                    var instanceSectionLength = instanceCount * INSTANCE_STRUCT_STRIDE;
                    var instanceSectionStart = instanceCurrentPosition - instanceSectionLength;

                    // don't need to align command buffer data, only instance data
                    renderList.tempInstanceBufferPosition = MathUtil.align(instanceCurrentPosition, this.instanceBuffer.getAlignment());

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

            int commandBufferCurrentPos = commandBufferSectionView.position();
            int instanceBufferCurrentPos = instanceBufferSectionView.position();
            for (var renderListIterator = renderLists.values().iterator(); renderListIterator.hasNext(); ) {
                var renderList = renderListIterator.next();

                if (renderList.batches.size() <= 0) {
                    renderListIterator.remove();
                    continue;
                }

                long mainCommandBufferOffset = commandBufferSection.getOffset() + commandBufferCurrentPos;
                long mainInstanceBufferOffset = instanceBufferSection.getOffset() + instanceBufferCurrentPos;
                for (var batch : renderList.batches) {
                    batch.commandBufferOffset += mainCommandBufferOffset;
                    batch.instanceBufferOffset += mainInstanceBufferOffset;
                }

                long tempCommandBufferLength = renderList.tempCommandBufferPosition;
                MemoryUtil.memCopy(
                        renderList.tempCommandBufferAddress,
                        commandBufferSectionAddress + commandBufferCurrentPos,
                        tempCommandBufferLength
                );
                commandBufferCurrentPos += tempCommandBufferLength;

                long tempInstanceBufferLength = renderList.tempInstanceBufferPosition;
                MemoryUtil.memCopy(
                        renderList.tempInstanceBufferAddress,
                        instanceBufferSectionAddress + instanceBufferCurrentPos,
                        tempInstanceBufferLength
                );
                instanceBufferCurrentPos += tempInstanceBufferLength;

            }
            commandBufferSectionView.position(commandBufferCurrentPos);
            instanceBufferSectionView.position(instanceBufferCurrentPos);
        }

        commandBufferSection.flushPartial();
        instanceBufferSection.flushPartial();

        return renderLists;
    }

    public void delete() {
        MemoryUtil.memFree(this.stackBuffer);
        this.commandBuffer.delete();
        this.instanceBuffer.delete();
    }

    public static class RenderList {
        private final Deque<ChunkRenderBatch> batches;
        private int largestVertexIndex;

        // these will be deallocated by the time construction is done
        private final long tempCommandBufferAddress;
        private long tempCommandBufferPosition;
        private final long tempInstanceBufferAddress;
        private long tempInstanceBufferPosition;

        private int currentInstanceCount;
        private int currentCommandCount;

        public RenderList(long tempCommandBufferAddress, long tempInstanceBufferAddress) {
            this.tempCommandBufferAddress = tempCommandBufferAddress;
            this.tempInstanceBufferAddress = tempInstanceBufferAddress;
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
        private long instanceBufferOffset;
        private long commandBufferOffset;

        public ChunkRenderBatch(Buffer vertexBuffer,
                                int vertexStride,
                                int instanceCount,
                                int commandCount,
                                long instanceBufferOffset,
                                long commandBufferOffset
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

        public long getInstanceBufferOffset() {
            return this.instanceBufferOffset;
        }

        public long getCommandBufferOffset() {
            return this.commandBufferOffset;
        }
    }

    private static int commandBufferPassSize(int alignment, SortedChunkLists list) {
        int size = 0;

        for (var bucket : list.unsorted()) {
            size += MathUtil.align((bucket.size() * ChunkMeshFace.COUNT) * COMMAND_STRUCT_STRIDE, alignment);
        }

        return size;
    }

    private static int instanceBufferPassSize(int alignment, SortedChunkLists list) {
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
