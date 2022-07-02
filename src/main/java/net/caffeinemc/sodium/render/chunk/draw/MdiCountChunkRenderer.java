package net.caffeinemc.sodium.render.chunk.draw;

import it.unimi.dsi.fastutil.objects.Reference2ReferenceArrayMap;
import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.Map;
import net.caffeinemc.gfx.api.buffer.Buffer;
import net.caffeinemc.gfx.api.buffer.MappedBufferFlags;
import net.caffeinemc.gfx.api.device.RenderDevice;
import net.caffeinemc.gfx.api.pipeline.Pipeline;
import net.caffeinemc.gfx.api.types.ElementFormat;
import net.caffeinemc.gfx.api.types.PrimitiveType;
import net.caffeinemc.gfx.util.buffer.DualStreamingBuffer;
import net.caffeinemc.gfx.util.buffer.StreamingBuffer;
import net.caffeinemc.sodium.SodiumClientMod;
import net.caffeinemc.sodium.render.chunk.RenderSection;
import net.caffeinemc.sodium.render.chunk.passes.ChunkRenderPass;
import net.caffeinemc.sodium.render.chunk.passes.ChunkRenderPassManager;
import net.caffeinemc.sodium.render.chunk.region.RenderRegion;
import net.caffeinemc.sodium.render.chunk.shader.ChunkShaderInterface;
import net.caffeinemc.sodium.render.chunk.state.UploadedChunkGeometry;
import net.caffeinemc.sodium.render.terrain.format.TerrainVertexType;
import net.caffeinemc.sodium.util.MathUtil;
import net.minecraft.util.math.ChunkSectionPos;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

public class MdiCountChunkRenderer extends MdiChunkRenderer {
    public static final int PARAMETER_STRUCT_STRIDE = Integer.BYTES;
    
    private final StreamingBuffer parameterBuffer;
    
    private Map<ChunkRenderPass, RenderList<MdiCountChunkRenderBatch>> renderLists;
    
    public MdiCountChunkRenderer(
            RenderDevice device,
            ChunkRenderPassManager renderPassManager,
            TerrainVertexType vertexType
    ) {
        super(device, renderPassManager, vertexType);
        
        int maxInFlightFrames = SodiumClientMod.options().advanced.cpuRenderAheadLimit + 1;
        
        this.parameterBuffer = new DualStreamingBuffer(
                device,
                1,
                Integer.BYTES * 1024, // 1024 calls to MDI+C should be plenty, but can expand if needed
                maxInFlightFrames,
                EnumSet.of(MappedBufferFlags.EXPLICIT_FLUSH)
        );
    }
    
    @Override
    public int getDeviceBufferObjects() {
        return super.getDeviceBufferObjects() + 1;
    }
    
    @Override
    public long getDeviceUsedMemory() {
        return super.getDeviceUsedMemory() + this.parameterBuffer.getDeviceUsedMemory();
    }
    
    @Override
    public long getDeviceAllocatedMemory() {
        return super.getDeviceUsedMemory() + this.parameterBuffer.getDeviceAllocatedMemory();
    }
    
    @Override
    public void createRenderLists(SortedChunkLists chunks, ChunkCameraContext camera, int frameIndex) {
        if (chunks.isEmpty()) {
            this.renderLists = null;
            return;
        }
        
        Collection<ChunkRenderPass> chunkRenderPasses = this.renderPassManager.getAllRenderPasses();
        int totalPasses = chunkRenderPasses.size();
        
        // setup buffers, resizing as needed
        int commandBufferPassSize = commandBufferPassSize(this.commandBuffer.getAlignment(), chunks);
        StreamingBuffer.WritableSection commandBufferSection = this.commandBuffer.getSection(
                frameIndex,
                commandBufferPassSize *
                totalPasses,
                false
        );
        ByteBuffer commandBufferSectionView = commandBufferSection.getView();
        long commandBufferSectionAddress = MemoryUtil.memAddress0(commandBufferSectionView);
        
        int instanceBufferPassSize = instanceBufferPassSize(this.uniformBufferInstanceData.getAlignment(), chunks);
        StreamingBuffer.WritableSection instanceBufferSection = this.uniformBufferInstanceData.getSection(
                frameIndex,
                instanceBufferPassSize *
                totalPasses,
                false
        );
        ByteBuffer instanceBufferSectionView = instanceBufferSection.getView();
        long instanceBufferSectionAddress = MemoryUtil.memAddress0(instanceBufferSectionView);
        
        int parameterBufferPassSize = parameterBufferPassSize(this.parameterBuffer.getAlignment(), chunks);
        StreamingBuffer.WritableSection parameterBufferSection = this.parameterBuffer.getSection(
                frameIndex,
                parameterBufferPassSize *
                totalPasses,
                false
        );
        ByteBuffer parameterBufferSectionView = parameterBufferSection.getView();
        long parameterBufferSectionAddress = MemoryUtil.memAddress0(parameterBufferSectionView);
        
        Map<ChunkRenderPass, RenderList<MdiCountChunkRenderBatch>> renderLists = new Reference2ReferenceArrayMap<>();
        
        // setup memory stack, which will be used to temporarily store buffer subsections to be copied to the main
        // buffer at the end in the correct order.
        int stackSize = (instanceBufferPassSize + commandBufferPassSize) * totalPasses;
        if (this.stackBuffer == null || this.stackBuffer.capacity() < stackSize) {
            MemoryUtil.memFree(this.stackBuffer); // this does nothing if the buffer is null
            this.stackBuffer = MemoryUtil.memAlloc(stackSize);
        }
        
        try (MemoryStack stack = MemoryStack.create(this.stackBuffer).push()) {
            for (ChunkRenderPass renderPass : chunkRenderPasses) {
                renderLists.put(
                        renderPass,
                        new RenderList<>(
                                stack.nmalloc(1, commandBufferPassSize),
                                stack.nmalloc(1, instanceBufferPassSize)
                        )
                );
            }
            
            boolean reverseOrder = false; // TODO: fix me
            
            for (Iterator<SortedChunkLists.Bucket> bucketIterator = chunks.sorted(reverseOrder); bucketIterator.hasNext(); ) {
                SortedChunkLists.Bucket bucket = bucketIterator.next();
                
                for (Iterator<RenderSection> sectionIterator = bucket.sorted(reverseOrder); sectionIterator.hasNext(); ) {
                    RenderSection section = sectionIterator.next();
                    
                    UploadedChunkGeometry geometry = section.getGeometry();
                    int baseVertex = geometry.segment.getOffset();
                    
                    int visibility = calculateVisibilityFlags(section.getBounds(), camera);
                    
                    for (UploadedChunkGeometry.PackedModel model : geometry.models) {
                        if ((model.visibilityBits & visibility) == 0) {
                            continue;
                        }
                        
                        RenderList<MdiCountChunkRenderBatch> renderList = renderLists.get(model.pass);
                        
                        if (renderList == null) { // very bad
                            continue;
                        }
                        
                        for (long range : model.ranges) {
                            if ((visibility & range) == 0) {
                                continue;
                            }
                            
                            int vertexCount = UploadedChunkGeometry.ModelPart.unpackVertexCount(range);
                            int firstVertex = baseVertex + UploadedChunkGeometry.ModelPart.unpackFirstVertex(range);
                            
                            long ptr = renderList.tempCommandBufferAddress + renderList.tempCommandBufferPosition;
                            MemoryUtil.memPutInt(ptr + 0, vertexCount);
                            MemoryUtil.memPutInt(ptr + 4, 1);
                            MemoryUtil.memPutInt(ptr + 8, 0);
                            MemoryUtil.memPutInt(ptr + 12, firstVertex); // baseVertex
                            MemoryUtil.memPutInt(ptr + 16, renderList.currentInstanceCount); // baseInstance
                            renderList.tempCommandBufferPosition += COMMAND_STRUCT_STRIDE;
                            renderList.currentCommandCount++;
                        }
                        
                        float x = getCameraTranslation(
                                ChunkSectionPos.getBlockCoord(section.getChunkX()),
                                camera.blockX,
                                camera.deltaX
                        );
                        float y = getCameraTranslation(
                                ChunkSectionPos.getBlockCoord(section.getChunkY()),
                                camera.blockY,
                                camera.deltaY
                        );
                        float z = getCameraTranslation(
                                ChunkSectionPos.getBlockCoord(section.getChunkZ()),
                                camera.blockZ,
                                camera.deltaZ
                        );
                        
                        long ptr = renderList.tempInstanceBufferAddress + renderList.tempInstanceBufferPosition;
                        MemoryUtil.memPutFloat(ptr + 0, x);
                        MemoryUtil.memPutFloat(ptr + 4, y);
                        MemoryUtil.memPutFloat(ptr + 8, z);
                        renderList.tempInstanceBufferPosition += INSTANCE_STRUCT_STRIDE;
                        renderList.currentInstanceCount++;
                        
                        renderList.largestVertexIndex = Math.max(
                                renderList.largestVertexIndex,
                                geometry.segment.getLength()
                        );
                    }
                }
                
                for (RenderList<MdiCountChunkRenderBatch> renderList : renderLists.values()) {
                    int instanceCount = renderList.currentInstanceCount;
                    int commandCount = renderList.currentCommandCount;
                    renderList.currentCommandCount = 0;
                    renderList.currentInstanceCount = 0;
                    
                    if (commandCount <= 0) {
                        continue;
                    }
                    
                    int parameterBufferOffset = parameterBufferSectionView.position();
                    long ptr = parameterBufferSectionAddress + parameterBufferOffset;
                    MemoryUtil.memPutInt(ptr, commandCount);
                    parameterBufferSectionView.position(parameterBufferOffset + PARAMETER_STRUCT_STRIDE);
                    
                    long commandCurrentPosition = renderList.tempCommandBufferPosition;
                    int commandSubsectionLength = commandCount * COMMAND_STRUCT_STRIDE;
                    long commandSubsectionStart = commandCurrentPosition - commandSubsectionLength;
                    renderList.tempCommandBufferPosition = MathUtil.align(
                            commandCurrentPosition,
                            this.commandBuffer.getAlignment()
                    );
                    
                    long instanceCurrentPosition = renderList.tempInstanceBufferPosition;
                    int instanceSubsectionLength = instanceCount * INSTANCE_STRUCT_STRIDE;
                    long instanceSubsectionStart = instanceCurrentPosition - instanceSubsectionLength;
                    renderList.tempInstanceBufferPosition = MathUtil.align(
                            instanceCurrentPosition,
                            this.uniformBufferInstanceData.getAlignment()
                    );
                    
                    RenderRegion region = bucket.region();
                    
                    renderList.getBatches().add(new MdiCountChunkRenderBatch(
                            region.vertexBuffers.getBufferObject(),
                            region.vertexBuffers.getStride(),
                            instanceCount,
                            commandCount,
                            instanceSubsectionStart,
                            commandSubsectionStart,
                            parameterBufferSection.getDeviceOffset() +
                            parameterBufferOffset
                    ));
                }
            }
            
            // copy all temporary instance and command subsections to their corresponding streaming buffer sections
            int commandBufferCurrentPos = commandBufferSectionView.position();
            int instanceBufferCurrentPos = instanceBufferSectionView.position();
            for (Iterator<RenderList<MdiCountChunkRenderBatch>> renderListIterator = renderLists.values()
                                                                                                .iterator(); renderListIterator.hasNext(); ) {
                RenderList<MdiCountChunkRenderBatch> renderList = renderListIterator.next();
                
                if (renderList.getBatches().size() <= 0) {
                    renderListIterator.remove();
                    continue;
                }
                
                long mainCommandBufferOffset = commandBufferSection.getDeviceOffset() + commandBufferCurrentPos;
                long mainInstanceBufferOffset = instanceBufferSection.getDeviceOffset() + instanceBufferCurrentPos;
                for (MdiCountChunkRenderBatch batch : renderList.getBatches()) {
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
        parameterBufferSection.flushPartial();
        
        this.renderLists = renderLists;
    }
    
    @Override
    public void delete() {
        super.delete();
        this.parameterBuffer.delete();
    }
    
    private static class MdiCountChunkRenderBatch extends MdiChunkRenderBatch {
        private final long parameterBufferOffset;
        
        public MdiCountChunkRenderBatch(
                Buffer vertexBuffer,
                int vertexStride,
                int instanceCount,
                int commandCount,
                long instanceBufferOffset,
                long commandBufferOffset,
                long parameterBufferOffset
        ) {
            super(vertexBuffer, vertexStride, instanceCount, commandCount, instanceBufferOffset, commandBufferOffset);
            this.parameterBufferOffset = parameterBufferOffset;
        }
        
        public long getParameterBufferOffset() {
            return this.parameterBufferOffset;
        }
    }
    
    private static int parameterBufferPassSize(int alignment, SortedChunkLists list) {
        return list.regionCount() * MathUtil.align(PARAMETER_STRUCT_STRIDE, alignment);
    }
    
    @Override
    public void render(ChunkRenderPass renderPass, ChunkRenderMatrices matrices, int frameIndex) {
        if (this.renderLists == null || !this.renderLists.containsKey(renderPass)) {
            return;
        }
        
        var renderList = this.renderLists.get(renderPass);
        this.indexBuffer.ensureCapacity(renderList.getLargestVertexIndex());
        
        Pipeline<ChunkShaderInterface, BufferTarget> pipeline = this.pipelines.get(renderPass);
        this.device.usePipeline(pipeline, (cmd, programInterface, pipelineState) -> {
            this.setupTextures(renderPass, pipelineState);
            this.setupUniforms(matrices, programInterface, pipelineState, frameIndex);
            
            cmd.bindCommandBuffer(this.commandBuffer.getBufferObject());
            cmd.bindElementBuffer(this.indexBuffer.getBuffer());
            cmd.bindParameterBuffer(this.parameterBuffer.getBufferObject());
            
            for (var batch : renderList.getBatches()) {
                pipelineState.bindBufferBlock(
                        programInterface.uniformInstanceData,
                        this.uniformBufferInstanceData.getBufferObject(),
                        batch.getInstanceBufferOffset(),
                        INSTANCE_DATA_SIZE
                        // the spec requires that the entire part of the UBO is filled completely, so lets just make the range the right size
                );
                
                cmd.bindVertexBuffer(BufferTarget.VERTICES, batch.getVertexBuffer(), 0, batch.getVertexStride());
                
                cmd.multiDrawElementsIndirectCount(
                        PrimitiveType.TRIANGLES,
                        ElementFormat.UNSIGNED_INT,
                        batch.getCommandBufferOffset(),
                        batch.getParameterBufferOffset(),
                        batch.getCommandCount(),
                        0
                );
            }
        });
    }
    
}