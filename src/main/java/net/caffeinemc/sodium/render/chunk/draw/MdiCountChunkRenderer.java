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
import net.caffeinemc.gfx.api.pipeline.Pipeline;
import net.caffeinemc.gfx.api.types.ElementFormat;
import net.caffeinemc.gfx.api.types.PrimitiveType;
import net.caffeinemc.gfx.util.buffer.DualStreamingBuffer;
import net.caffeinemc.gfx.util.buffer.StreamingBuffer;
import net.caffeinemc.sodium.SodiumClientMod;
import net.caffeinemc.sodium.render.buffer.VertexRange;
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
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.system.Pointer;

public class MdiCountChunkRenderer extends MdiChunkRenderer {
    public static final int PARAMETER_STRUCT_STRIDE = Integer.BYTES;

    private final StreamingBuffer parameterBuffer;

    private Collection<MdiCountChunkRenderBatch>[] renderLists;

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
    
        ChunkRenderPass[] chunkRenderPasses = this.renderPassManager.getAllRenderPasses();
        int totalPasses = chunkRenderPasses.length;
    
        // setup buffers, resizing as needed
        int commandBufferPassSize = commandBufferPassSize(this.commandBuffer.getAlignment(), chunks);
        StreamingBuffer.WritableSection commandBufferSection = this.commandBuffer.getSection(
                frameIndex,
                commandBufferPassSize * totalPasses,
                false
        );
        ByteBuffer commandBufferSectionView = commandBufferSection.getView();
        long commandBufferSectionAddress = MemoryUtil.memAddress0(commandBufferSectionView);
    
        int instanceBufferPassSize = instanceBufferPassSize(this.uniformBufferInstanceData.getAlignment(), chunks);
        StreamingBuffer.WritableSection instanceBufferSection = this.uniformBufferInstanceData.getSection(
                frameIndex,
                instanceBufferPassSize * totalPasses,
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
    
        int largestVertexIndex = 0;
        int commandBufferPosition = commandBufferSectionView.position();
        int instanceBufferPosition = instanceBufferSectionView.position();
        int parameterBufferPosition = parameterBufferSectionView.position();
    
        @SuppressWarnings("unchecked")
        Collection<MdiCountChunkRenderBatch>[] renderLists = new Collection[totalPasses];
    
        for (int passId = 0; passId < chunkRenderPasses.length; passId++) {
            ChunkRenderPass renderPass = chunkRenderPasses[passId];
            Deque<MdiCountChunkRenderBatch> renderList = new ArrayDeque<>(16); // just an estimate
        
            boolean reverseOrder = renderPass.isTranslucent();
        
            for (Iterator<SortedChunkLists.RegionBucket> regionIterator = chunks.sortedRegionBuckets(reverseOrder); regionIterator.hasNext(); ) {
                SortedChunkLists.RegionBucket regionBucket = regionIterator.next();
            
                int batchInstanceCount = 0;
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
                
                    VertexRange[] modelParts = model.getModelParts();
                    for (int dir = 0; dir < modelParts.length; dir++) {
                        if ((visibility & (1 << dir)) == 0) {
                            continue;
                        }
                    
                        VertexRange modelPart = modelParts[dir];
                    
                        if (modelPart == null) {
                            continue;
                        }
                    
                        long ptr = commandBufferSectionAddress + commandBufferPosition;
                        MemoryUtil.memPutInt(ptr + 0, modelPart.indexCount());
                        MemoryUtil.memPutInt(ptr + 4, 1);
                        MemoryUtil.memPutInt(ptr + 8, 0);
                        MemoryUtil.memPutInt(ptr + 12, baseVertex + modelPart.firstVertex()); // baseVertex
                        MemoryUtil.memPutInt(ptr + 16, batchInstanceCount); // baseInstance
                        commandBufferPosition += COMMAND_STRUCT_STRIDE;
                        batchCommandCount++;
                    }
                
                    // TODO: should only need instance buffer data written once or twice, not for every render pass
                
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
                
                    long ptr = instanceBufferSectionAddress + instanceBufferPosition;
                    MemoryUtil.memPutFloat(ptr + 0, x);
                    MemoryUtil.memPutFloat(ptr + 4, y);
                    MemoryUtil.memPutFloat(ptr + 8, z);
                    instanceBufferPosition += INSTANCE_STRUCT_STRIDE;
                    batchInstanceCount++;
                
                    largestVertexIndex = Math.max(largestVertexIndex, geometry.segment.getLength());
                }
            
                if (batchCommandCount == 0) {
                    continue;
                }
                
                long ptr = parameterBufferSectionAddress + parameterBufferPosition;
                MemoryUtil.memPutInt(ptr, batchCommandCount);
    
                int commandSubsectionLength = batchCommandCount * COMMAND_STRUCT_STRIDE;
                long commandSubsectionStart = commandBufferSection.getDeviceOffset()
                                              + commandBufferPosition - commandSubsectionLength;
                commandBufferPosition = MathUtil.align(
                        commandBufferPosition,
                        this.commandBuffer.getAlignment()
                );
    
                int instanceSubsectionLength = batchInstanceCount * INSTANCE_STRUCT_STRIDE;
                long instanceSubsectionStart = instanceBufferSection.getDeviceOffset()
                                               + instanceBufferPosition - instanceSubsectionLength;
                instanceBufferPosition = MathUtil.align(
                        instanceBufferPosition,
                        this.uniformBufferInstanceData.getAlignment()
                );
    
                RenderRegion region = regionBucket.getRegion();
    
                renderList.add(new MdiCountChunkRenderBatch(
                        region.vertexBuffers.getBufferObject(),
                        region.vertexBuffers.getStride(),
                        batchInstanceCount,
                        batchCommandCount,
                        instanceSubsectionStart,
                        commandSubsectionStart,
                        parameterBufferSection.getDeviceOffset() + parameterBufferPosition
                ));
    
                // set this here so the batch gets the correct value
                parameterBufferPosition += PARAMETER_STRUCT_STRIDE;
            }
    
            if (!renderList.isEmpty()) {
                renderLists[passId] = renderList;
            }
        }
    
        commandBufferSectionView.position(commandBufferPosition);
        instanceBufferSectionView.position(instanceBufferPosition);
        parameterBufferSectionView.position(parameterBufferPosition);
    
        commandBufferSection.flushPartial();
        instanceBufferSection.flushPartial();
        parameterBufferSection.flushPartial();
    
        this.indexBuffer.ensureCapacity(largestVertexIndex);
    
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
        return list.getRegionCount() * MathUtil.align(PARAMETER_STRUCT_STRIDE, alignment);
    }

    @Override
    public void render(ChunkRenderPass renderPass, ChunkRenderMatrices matrices, int frameIndex) {
        // make sure a render list was created for this pass, if any
        if (this.renderLists == null) {
            return;
        }
    
        int passId = renderPass.getId();
        if (passId < 0 || this.renderLists.length < passId) {
            return;
        }
    
        var renderList = this.renderLists[passId];
        if (renderList == null) {
            return;
        }
    
        // if the render list exists, the pipeline probably exists (unless a new render pass was added without a reload)
        Pipeline<ChunkShaderInterface, BufferTarget> pipeline = this.pipelines[passId];
        
        this.device.usePipeline(pipeline, (cmd, programInterface, pipelineState) -> {
            this.setupTextures(renderPass, pipelineState);
            this.setupUniforms(matrices, programInterface, pipelineState, frameIndex);

            cmd.bindCommandBuffer(this.commandBuffer.getBufferObject());
            cmd.bindElementBuffer(this.indexBuffer.getBuffer());
            cmd.bindParameterBuffer(this.parameterBuffer.getBufferObject());
    
            for (var batch : renderList) {
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