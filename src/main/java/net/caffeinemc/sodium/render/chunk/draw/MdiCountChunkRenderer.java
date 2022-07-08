package net.caffeinemc.sodium.render.chunk.draw;

import java.nio.ByteBuffer;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.EnumSet;
import java.util.Iterator;
import net.caffeinemc.gfx.api.buffer.Buffer;
import net.caffeinemc.gfx.api.buffer.MappedBufferFlags;
import net.caffeinemc.gfx.api.device.RenderDevice;
import net.caffeinemc.gfx.api.device.commands.RenderCommandList;
import net.caffeinemc.gfx.api.pipeline.Pipeline;
import net.caffeinemc.gfx.api.pipeline.PipelineState;
import net.caffeinemc.gfx.api.types.ElementFormat;
import net.caffeinemc.gfx.api.types.PrimitiveType;
import net.caffeinemc.gfx.util.buffer.DualStreamingBuffer;
import net.caffeinemc.gfx.util.buffer.StreamingBuffer;
import net.caffeinemc.sodium.SodiumClientMod;
import net.caffeinemc.sodium.render.buffer.ModelRange;
import net.caffeinemc.sodium.render.chunk.RenderSection;
import net.caffeinemc.sodium.render.chunk.passes.ChunkRenderPass;
import net.caffeinemc.sodium.render.chunk.passes.ChunkRenderPassManager;
import net.caffeinemc.sodium.render.chunk.region.RenderRegion;
import net.caffeinemc.sodium.render.chunk.shader.ChunkShaderInterface;
import net.caffeinemc.sodium.render.chunk.state.ChunkPassModel;
import net.caffeinemc.sodium.render.chunk.state.UploadedChunkGeometry;
import net.caffeinemc.sodium.render.terrain.format.TerrainVertexType;
import net.caffeinemc.sodium.util.MathUtil;
import net.minecraft.util.math.ChunkSectionPos;
import org.lwjgl.system.MemoryUtil;

public class MdiCountChunkRenderer extends MdiChunkRenderer<MdiCountChunkRenderer.MdiCountChunkRenderBatch> {
    public static final int DRAW_COUNTS_STRUCT_STRIDE = Integer.BYTES;

    private final StreamingBuffer drawCountsBuffer;

    public MdiCountChunkRenderer(
            RenderDevice device,
            ChunkRenderPassManager renderPassManager,
            TerrainVertexType vertexType
    ) {
        super(device, renderPassManager, vertexType);

        int maxInFlightFrames = SodiumClientMod.options().advanced.cpuRenderAheadLimit + 1;

        this.drawCountsBuffer = new DualStreamingBuffer(
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
        return super.getDeviceUsedMemory() +
               this.drawCountsBuffer.getDeviceUsedMemory();
    }

    @Override
    public long getDeviceAllocatedMemory() {
        return super.getDeviceUsedMemory() +
               this.drawCountsBuffer.getDeviceAllocatedMemory();
    }

    @Override
    public void createRenderLists(SortedChunkLists chunks, ChunkCameraContext camera, int frameIndex) {
        if (chunks.isEmpty()) {
            this.renderLists = null;
            return;
        }
    
        ChunkRenderPass[] chunkRenderPasses = this.renderPassManager.getAllRenderPasses();
        int totalPasses = chunkRenderPasses.length;
    
        boolean useBlockFaceCulling = SodiumClientMod.options().performance.useBlockFaceCulling;
    
        // setup buffers, resizing as needed
        int commandBufferPassSize = commandBufferPassSize(this.commandBuffer.getAlignment(), chunks);
        StreamingBuffer.WritableSection commandBufferSection = this.commandBuffer.getSection(
                frameIndex,
                commandBufferPassSize * totalPasses,
                false
        );
        ByteBuffer commandBufferSectionView = commandBufferSection.getView();
        long commandBufferSectionAddress = MemoryUtil.memAddress0(commandBufferSectionView);
    
        int transformsBufferPassSize = indexedTransformsBufferPassSize(this.uniformBufferChunkTransforms.getAlignment(), chunks);
        StreamingBuffer.WritableSection transformsBufferSection = this.uniformBufferChunkTransforms.getSection(
                frameIndex,
                transformsBufferPassSize * totalPasses,
                false
        );
        ByteBuffer transformsBufferSectionView = transformsBufferSection.getView();
        long transformsBufferSectionAddress = MemoryUtil.memAddress0(transformsBufferSectionView);
    
        int drawCountsBufferPassSize = drawCountsBufferPassSize(this.drawCountsBuffer.getAlignment(), chunks);
        StreamingBuffer.WritableSection drawCountsBufferSection = this.drawCountsBuffer.getSection(
                frameIndex,
                drawCountsBufferPassSize *
                totalPasses,
                false
        );
        ByteBuffer drawCountsBufferSectionView = drawCountsBufferSection.getView();
        long drawCountsBufferSectionAddress = MemoryUtil.memAddress0(drawCountsBufferSectionView);
    
        int largestVertexIndex = 0;
        int commandBufferPosition = commandBufferSectionView.position();
        int transformsBufferPosition = transformsBufferSectionView.position();
        int drawCountsBufferPosition = drawCountsBufferSectionView.position();
    
        @SuppressWarnings("unchecked")
        Collection<MdiCountChunkRenderer.MdiCountChunkRenderBatch>[] renderLists = new Collection[totalPasses];
    
        for (int passId = 0; passId < chunkRenderPasses.length; passId++) {
            ChunkRenderPass renderPass = chunkRenderPasses[passId];
            Deque<MdiCountChunkRenderer.MdiCountChunkRenderBatch> renderList = new ArrayDeque<>(16); // just an estimate
        
            boolean reverseOrder = renderPass.isTranslucent();
        
            for (Iterator<SortedChunkLists.RegionBucket> regionIterator = chunks.sortedRegionBuckets(reverseOrder); regionIterator.hasNext(); ) {
                SortedChunkLists.RegionBucket regionBucket = regionIterator.next();
            
                int batchTransformCount = 0;
                int batchCommandCount = 0;
            
                for (Iterator<RenderSection> sectionIterator = regionBucket.sortedSections(reverseOrder); sectionIterator.hasNext(); ) {
                    RenderSection section = sectionIterator.next();
                
                    UploadedChunkGeometry geometry = section.getGeometry();
                    if (geometry.models == null) {
                        continue;
                    }
                
                    int baseVertex = geometry.segment.getOffset();
                
                    int visibility = calculateVisibilityFlags(section.getBounds(), camera);
                
                    ChunkPassModel model = geometry.models[passId];
                
                    if (model == null || (model.getVisibilityBits() & visibility) == 0) {
                        continue;
                    }
                
                    ModelRange[] modelParts = model.getModelParts();
                    for (int dir = 0; dir < modelParts.length; dir++) {
                        if (useBlockFaceCulling && (visibility & (1 << dir)) == 0) {
                            continue;
                        }
                    
                        ModelRange modelPart = modelParts[dir];
                    
                        if (modelPart == null) {
                            continue;
                        }
                    
                        long ptr = commandBufferSectionAddress + commandBufferPosition;
                        MemoryUtil.memPutInt(ptr + 0, modelPart.indexCount());
                        MemoryUtil.memPutInt(ptr + 4, 1);
                        MemoryUtil.memPutInt(ptr + 8, 0);
                        MemoryUtil.memPutInt(ptr + 12, baseVertex + modelPart.firstVertex()); // baseVertex
                        MemoryUtil.memPutInt(ptr + 16, batchTransformCount); // baseInstance
                        commandBufferPosition += COMMAND_STRUCT_STRIDE;
                        batchCommandCount++;
                    }
                
                    // TODO: should only need transforms buffer data written once or twice, not for every render pass
                
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
                
                    long ptr = transformsBufferSectionAddress + transformsBufferPosition;
                    MemoryUtil.memPutFloat(ptr + 0, x);
                    MemoryUtil.memPutFloat(ptr + 4, y);
                    MemoryUtil.memPutFloat(ptr + 8, z);
                    transformsBufferPosition += TRANSFORM_STRUCT_STRIDE;
                    batchTransformCount++;
                
                    largestVertexIndex = Math.max(largestVertexIndex, geometry.segment.getLength());
                }
            
                if (batchCommandCount == 0) {
                    continue;
                }
            
                long ptr = drawCountsBufferSectionAddress + drawCountsBufferPosition;
                MemoryUtil.memPutInt(ptr, batchCommandCount);
            
                int commandSubsectionLength = batchCommandCount * COMMAND_STRUCT_STRIDE;
                long commandSubsectionStart = commandBufferSection.getDeviceOffset()
                                              + commandBufferPosition - commandSubsectionLength;
                commandBufferPosition = MathUtil.align(
                        commandBufferPosition,
                        this.commandBuffer.getAlignment()
                );
            
                int transformsSubsectionLength = batchTransformCount * TRANSFORM_STRUCT_STRIDE;
                long transformsSubsectionStart = transformsBufferSection.getDeviceOffset()
                                                + transformsBufferPosition - transformsSubsectionLength;
                transformsBufferPosition = MathUtil.align(
                        transformsBufferPosition,
                        this.uniformBufferChunkTransforms.getAlignment()
                );
                
                long drawCountsSubsectionStart = drawCountsBufferSection.getDeviceOffset() + drawCountsBufferPosition;
            
                RenderRegion region = regionBucket.getRegion();
            
                renderList.add(new MdiCountChunkRenderBatch(
                        region.vertexBuffers.getBufferObject(),
                        region.vertexBuffers.getStride(),
                        batchCommandCount,
                        transformsSubsectionStart,
                        commandSubsectionStart,
                        drawCountsSubsectionStart
                ));
            
                // set this here so the batch gets the correct value
                drawCountsBufferPosition = MathUtil.align(
                        drawCountsBufferPosition + DRAW_COUNTS_STRUCT_STRIDE,
                        this.drawCountsBuffer.getAlignment()
                );
            }
        
            if (!renderList.isEmpty()) {
                renderLists[passId] = renderList;
            }
        }
    
        commandBufferSectionView.position(commandBufferPosition);
        transformsBufferSectionView.position(transformsBufferPosition);
        drawCountsBufferSectionView.position(drawCountsBufferPosition);
    
        commandBufferSection.flushPartial();
        transformsBufferSection.flushPartial();
        drawCountsBufferSection.flushPartial();
    
        this.indexBuffer.ensureCapacity(largestVertexIndex);
    
        this.renderLists = renderLists;
    }

    @Override
    public void delete() {
        super.delete();
        this.drawCountsBuffer.delete();
    }

    public static class MdiCountChunkRenderBatch extends MdiChunkRenderBatch {
        private final long drawCountsBufferOffset;

        public MdiCountChunkRenderBatch(
                Buffer vertexBuffer,
                int vertexStride,
                int commandCount,
                long transformsBufferOffset,
                long commandBufferOffset,
                long drawCountsBufferOffset
        ) {
            super(vertexBuffer, vertexStride, commandCount, transformsBufferOffset, commandBufferOffset);
            this.drawCountsBufferOffset = drawCountsBufferOffset;
        }

        public long getDrawCountsBufferOffset() {
            return this.drawCountsBufferOffset;
        }
    }

    private static int drawCountsBufferPassSize(int alignment, SortedChunkLists list) {
        return list.getRegionCount() * MathUtil.align(DRAW_COUNTS_STRUCT_STRIDE, alignment);
    }
    
    @Override
    protected void setupPerRenderList(
            ChunkRenderPass renderPass,
            ChunkRenderMatrices matrices,
            int frameIndex,
            Pipeline<ChunkShaderInterface, BufferTarget> pipeline,
            RenderCommandList<BufferTarget> commandList,
            ChunkShaderInterface programInterface,
            PipelineState pipelineState
    ) {
        super.setupPerRenderList(
                renderPass,
                matrices,
                frameIndex,
                pipeline,
                commandList,
                programInterface,
                pipelineState
        );
    
        commandList.bindParameterBuffer(this.drawCountsBuffer.getBufferObject());
    }
    
    @Override
    protected void issueDraw(
            ChunkRenderPass renderPass,
            ChunkRenderMatrices matrices,
            int frameIndex,
            Pipeline<ChunkShaderInterface, BufferTarget> pipeline,
            RenderCommandList<BufferTarget> commandList,
            ChunkShaderInterface programInterface,
            PipelineState pipelineState,
            MdiCountChunkRenderBatch batch
    ) {
        commandList.multiDrawElementsIndirectCount(
                PrimitiveType.TRIANGLES,
                ElementFormat.UNSIGNED_INT,
                batch.getCommandBufferOffset(),
                batch.getDrawCountsBufferOffset(),
                batch.getCommandCount(),
                0
        );
    }
    
    @Override
    public String getDebugName() {
        return "MDIC";
    }
}