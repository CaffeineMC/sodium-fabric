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
import net.caffeinemc.gfx.api.pipeline.RenderPipeline;
import net.caffeinemc.gfx.api.pipeline.PipelineState;
import net.caffeinemc.gfx.api.types.ElementFormat;
import net.caffeinemc.gfx.api.types.PrimitiveType;
import net.caffeinemc.gfx.util.buffer.streaming.DualStreamingBuffer;
import net.caffeinemc.gfx.util.buffer.streaming.StreamingBuffer;
import net.caffeinemc.sodium.SodiumClientMod;
import net.caffeinemc.sodium.render.buffer.ModelRange;
import net.caffeinemc.sodium.render.buffer.arena.BufferSegment;
import net.caffeinemc.sodium.render.chunk.RenderSection;
import net.caffeinemc.sodium.render.chunk.passes.ChunkRenderPass;
import net.caffeinemc.sodium.render.chunk.passes.ChunkRenderPassManager;
import net.caffeinemc.sodium.render.chunk.region.RenderRegion;
import net.caffeinemc.sodium.render.chunk.shader.ChunkShaderInterface;
import net.caffeinemc.sodium.render.chunk.state.ChunkPassModel;
import net.caffeinemc.sodium.render.shader.ShaderConstants;
import net.caffeinemc.sodium.render.terrain.format.TerrainVertexType;
import net.caffeinemc.sodium.render.terrain.quad.properties.ChunkMeshFace;
import net.caffeinemc.sodium.util.MathUtil;
import net.minecraft.util.math.ChunkSectionPos;
import org.lwjgl.system.MemoryUtil;

public class MdiChunkRenderer<B extends MdiChunkRenderer.MdiChunkRenderBatch> extends AbstractMdChunkRenderer<B> {
    public static final int COMMAND_STRUCT_STRIDE = 5 * Integer.BYTES;

    protected final StreamingBuffer commandBuffer;

    public MdiChunkRenderer(
            RenderDevice device,
            ChunkRenderPassManager renderPassManager,
            TerrainVertexType vertexType
    ) {
        super(device, renderPassManager, vertexType);

        int maxInFlightFrames = SodiumClientMod.options().advanced.cpuRenderAheadLimit + 1;
        
        this.commandBuffer = new DualStreamingBuffer(
                device,
                1,
                1048576, // start with 1 MiB and expand from there if needed
                maxInFlightFrames,
                EnumSet.of(MappedBufferFlags.EXPLICIT_FLUSH)
        );
    }
    
    @Override
    protected ShaderConstants.Builder addAdditionalShaderConstants(ShaderConstants.Builder constants) {
        constants.add("BASE_INSTANCE_INDEX");
        constants.add("MAX_BATCH_SIZE", String.valueOf(RenderRegion.REGION_SIZE));
        return constants;
    }

    @Override
    public int getDeviceBufferObjects() {
        return super.getDeviceBufferObjects() + 1;
    }

    @Override
    public long getDeviceUsedMemory() {
        return super.getDeviceUsedMemory() +
               this.commandBuffer.getDeviceUsedMemory();
    }

    @Override
    public long getDeviceAllocatedMemory() {
        return super.getDeviceAllocatedMemory() +
               this.commandBuffer.getDeviceAllocatedMemory();
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

        int transformBufferPassSize = indexedTransformsBufferPassSize(this.uniformBufferChunkTransforms.getAlignment(), chunks);
        StreamingBuffer.WritableSection transformBufferSection = this.uniformBufferChunkTransforms.getSection(
                frameIndex,
                transformBufferPassSize * totalPasses,
                false
        );
        ByteBuffer transformBufferSectionView = transformBufferSection.getView();
        long transformBufferSectionAddress = MemoryUtil.memAddress0(transformBufferSectionView);
    
        int largestVertexIndex = 0;
        int commandBufferPosition = commandBufferSectionView.position();
        int transformBufferPosition = transformBufferSectionView.position();
    
        @SuppressWarnings("unchecked")
        Collection<B>[] renderLists = new Collection[totalPasses];
    
        for (int passId = 0; passId < chunkRenderPasses.length; passId++) {
            ChunkRenderPass renderPass = chunkRenderPasses[passId];
            Deque<B> renderList = new ArrayDeque<>(16); // just an estimate
    
            boolean reverseOrder = renderPass.isTranslucent();
            
            for (Iterator<SortedChunkLists.RegionBucket> regionIterator = chunks.sortedRegionBuckets(reverseOrder); regionIterator.hasNext(); ) {
                SortedChunkLists.RegionBucket regionBucket = regionIterator.next();
                
                int batchTransformCount = 0;
                int batchCommandCount = 0;
            
                for (Iterator<RenderSection> sectionIterator = regionBucket.sortedSections(reverseOrder); sectionIterator.hasNext(); ) {
                    RenderSection section = sectionIterator.next();
    
                    long uploadedSegment = section.getUploadedGeometrySegment();
    
                    if (uploadedSegment == BufferSegment.INVALID) {
                        continue;
                    }
                    
                    int baseVertex = BufferSegment.getOffset(uploadedSegment);
                
                    int visibility = calculateVisibilityFlags(section.getData().bounds, camera);
                    
                    ChunkPassModel model = section.getData().models[passId];
                    
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
                    
                    // TODO: should only need transform buffer data written once or twice, not for every render pass
                
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
                
                    long ptr = transformBufferSectionAddress + transformBufferPosition;
                    MemoryUtil.memPutFloat(ptr + 0, x);
                    MemoryUtil.memPutFloat(ptr + 4, y);
                    MemoryUtil.memPutFloat(ptr + 8, z);
                    transformBufferPosition += TRANSFORM_STRUCT_STRIDE;
                    batchTransformCount++;
                
                    largestVertexIndex = Math.max(largestVertexIndex, BufferSegment.getLength(uploadedSegment));
                }
            
                if (batchCommandCount == 0) {
                    continue;
                }
            
                int commandSubsectionLength = batchCommandCount * COMMAND_STRUCT_STRIDE;
                long commandSubsectionStart = commandBufferSection.getDeviceOffset()
                                              + commandBufferPosition - commandSubsectionLength;
                commandBufferPosition = MathUtil.align(
                        commandBufferPosition,
                        this.commandBuffer.getAlignment()
                );
            
                int transformSubsectionLength = batchTransformCount * TRANSFORM_STRUCT_STRIDE;
                long transformSubsectionStart = transformBufferSection.getDeviceOffset()
                                               + transformBufferPosition - transformSubsectionLength;
                transformBufferPosition = MathUtil.align(
                        transformBufferPosition,
                        this.uniformBufferChunkTransforms.getAlignment()
                );
            
                RenderRegion region = regionBucket.getRegion();
    
                // WHY IS THIS NEEDED???
                //noinspection unchecked
                renderList.add((B) new MdiChunkRenderBatch(
                        region.vertexBuffers.getBufferObject(),
                        region.vertexBuffers.getStride(),
                        batchCommandCount,
                        transformSubsectionStart,
                        commandSubsectionStart
                ));
            
            }
        
            if (!renderList.isEmpty()) {
                renderLists[passId] = renderList;
            }
        }
        
        commandBufferSectionView.position(commandBufferPosition);
        transformBufferSectionView.position(transformBufferPosition);

        commandBufferSection.flushPartial();
        transformBufferSection.flushPartial();
        
        this.indexBuffer.ensureCapacity(largestVertexIndex);
        
        this.renderLists = renderLists;
    }

    @Override
    public void delete() {
        super.delete();
        this.commandBuffer.delete();
    }

    protected static class MdiChunkRenderBatch extends MdChunkRenderBatch {
        protected final long commandBufferOffset;

        public MdiChunkRenderBatch(
                Buffer vertexBuffer,
                int vertexStride,
                int commandCount,
                long transformBufferOffset,
                long commandBufferOffset
        ) {
            super(vertexBuffer, vertexStride, commandCount, transformBufferOffset);
            this.commandBufferOffset = commandBufferOffset;
        }

        public long getCommandBufferOffset() {
            return this.commandBufferOffset;
        }
    }

    protected static int commandBufferPassSize(int alignment, SortedChunkLists list) {
        int size = 0;

        for (SortedChunkLists.RegionBucket regionBucket : list.unsortedRegionBuckets()) {
            size += MathUtil.align((regionBucket.getSectionCount() * ChunkMeshFace.COUNT) * COMMAND_STRUCT_STRIDE, alignment);
        }

        return size;
    }
    
    protected static int indexedTransformsBufferPassSize(int alignment, SortedChunkLists list) {
        int size = 0;
        
        for (SortedChunkLists.RegionBucket regionBucket : list.unsortedRegionBuckets()) {
            size += MathUtil.align(regionBucket.getSectionCount() * TRANSFORM_STRUCT_STRIDE, alignment);
        }
        
        return size;
    }
    
    @Override
    protected void setupPerRenderList(
            ChunkRenderPass renderPass,
            ChunkRenderMatrices matrices,
            int frameIndex,
            RenderPipeline<ChunkShaderInterface, BufferTarget> renderPipeline,
            RenderCommandList<BufferTarget> commandList,
            ChunkShaderInterface programInterface,
            PipelineState pipelineState
    ) {
        super.setupPerRenderList(
                renderPass,
                matrices,
                frameIndex,
                renderPipeline,
                commandList,
                programInterface,
                pipelineState
        );
        
        commandList.bindCommandBuffer(this.commandBuffer.getBufferObject());
    }
    
    @Override
    protected void setupPerBatch(
            ChunkRenderPass renderPass,
            ChunkRenderMatrices matrices,
            int frameIndex,
            RenderPipeline<ChunkShaderInterface, BufferTarget> renderPipeline,
            RenderCommandList<BufferTarget> commandList,
            ChunkShaderInterface programInterface,
            PipelineState pipelineState,
            B batch
    ) {
        super.setupPerBatch(
                renderPass,
                matrices,
                frameIndex,
                renderPipeline,
                commandList,
                programInterface,
                pipelineState,
                batch
        );
    
        pipelineState.bindBufferBlock(
                programInterface.uniformChunkTransforms,
                this.uniformBufferChunkTransforms.getBufferObject(),
                batch.getTransformsBufferOffset(),
                RenderRegion.REGION_SIZE * TRANSFORM_STRUCT_STRIDE // the spec requires that the entire part of the UBO is filled completely, so lets just make the range the right size
        );
    }
    
    @Override
    protected void issueDraw(
            ChunkRenderPass renderPass,
            ChunkRenderMatrices matrices,
            int frameIndex,
            RenderPipeline<ChunkShaderInterface, BufferTarget> renderPipeline,
            RenderCommandList<BufferTarget> commandList,
            ChunkShaderInterface programInterface,
            PipelineState pipelineState,
            B batch
    ) {
        commandList.multiDrawElementsIndirect(
                PrimitiveType.TRIANGLES,
                ElementFormat.UNSIGNED_INT,
                batch.getCommandBufferOffset(),
                batch.getCommandCount(),
                0
        );
    }
    
    @Override
    public String getDebugName() {
        return "MDI";
    }
}
