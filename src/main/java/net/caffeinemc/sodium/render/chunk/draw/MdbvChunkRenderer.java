package net.caffeinemc.sodium.render.chunk.draw;

import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.longs.LongList;
import java.nio.ByteBuffer;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.List;
import net.caffeinemc.gfx.api.buffer.Buffer;
import net.caffeinemc.gfx.api.device.RenderDevice;
import net.caffeinemc.gfx.api.device.commands.RenderCommandList;
import net.caffeinemc.gfx.api.pipeline.PipelineState;
import net.caffeinemc.gfx.api.pipeline.RenderPipeline;
import net.caffeinemc.gfx.api.types.ElementFormat;
import net.caffeinemc.gfx.api.types.PrimitiveType;
import net.caffeinemc.gfx.util.buffer.streaming.StreamingBuffer;
import net.caffeinemc.sodium.render.buffer.arena.BufferSegment;
import net.caffeinemc.sodium.render.chunk.passes.ChunkRenderPass;
import net.caffeinemc.sodium.render.chunk.passes.ChunkRenderPassManager;
import net.caffeinemc.sodium.render.chunk.region.RenderRegion;
import net.caffeinemc.sodium.render.chunk.shader.ChunkShaderInterface;
import net.caffeinemc.sodium.render.shader.ShaderConstants;
import net.caffeinemc.sodium.render.terrain.format.TerrainVertexType;
import net.caffeinemc.sodium.render.terrain.quad.properties.ChunkMeshFace;
import net.caffeinemc.sodium.util.MathUtil;
import net.minecraft.util.math.ChunkSectionPos;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.system.Pointer;

public class MdbvChunkRenderer extends AbstractMdChunkRenderer<MdbvChunkRenderer.MdbvChunkRenderBatch> {
    
    protected long indexCountsBufferPtr;
    protected long baseVerticesBufferPtr;
    protected long indexOffsetsBufferPtr;
    
    protected int sectionFacesAllocated;
    
    public MdbvChunkRenderer(
            RenderDevice device,
            ChunkRenderPassManager renderPassManager,
            TerrainVertexType vertexType
    ) {
        super(device, renderPassManager, vertexType);
    
        this.sectionFacesAllocated = 1024; // can be resized when needed, just a guess
        this.allocateCPUBuffers();
    }
    
    protected void allocateCPUBuffers() {
        // These only need to store the size of one pass, as the buffers are copied when the commands are executed.
        this.indexCountsBufferPtr = MemoryUtil.nmemAlloc((long) this.sectionFacesAllocated * Integer.BYTES);
        this.baseVerticesBufferPtr = MemoryUtil.nmemAlloc((long) this.sectionFacesAllocated * Integer.BYTES);
        // because we always want the value to be 0, we can just calloc and never have to modify it
        this.indexOffsetsBufferPtr = MemoryUtil.nmemCalloc(1, (long) this.sectionFacesAllocated * Pointer.POINTER_SIZE);
    }
    
    protected void freeCPUBuffers() {
        MemoryUtil.nmemFree(this.indexCountsBufferPtr);
        MemoryUtil.nmemFree(this.indexOffsetsBufferPtr);
        MemoryUtil.nmemFree(this.baseVerticesBufferPtr);
        this.indexCountsBufferPtr = MemoryUtil.NULL;
        this.indexOffsetsBufferPtr = MemoryUtil.NULL;
        this.baseVerticesBufferPtr = MemoryUtil.NULL;
    }
    
    @Override
    protected ShaderConstants.Builder addAdditionalShaderConstants(ShaderConstants.Builder constants) {
        constants.add("MAX_BATCH_SIZE", String.valueOf(RenderRegion.REGION_SIZE * ChunkMeshFace.COUNT));
        return constants;
    }
    
    @Override
    public void createRenderLists(SortedTerrainLists lists, ChunkCameraContext camera, int frameIndex) {
        if (lists.isEmpty()) {
            this.renderLists = null;
            return;
        }
    
        ChunkRenderPass[] chunkRenderPasses = this.renderPassManager.getAllRenderPasses();
        int totalPasses = chunkRenderPasses.length;
    
        // setup buffers, resizing as needed
        int transformsBufferPassSize = unindexedTransformsBufferSize(this.uniformBufferChunkTransforms.getAlignment(), lists);
        StreamingBuffer.WritableSection transformsBufferSection = this.uniformBufferChunkTransforms.getSection(
                frameIndex,
                transformsBufferPassSize,
                false
        );
        ByteBuffer transformsBufferSectionView = transformsBufferSection.getView();
        long transformsBufferSectionAddress = MemoryUtil.memAddress0(transformsBufferSectionView);
        
        int maxSectionFaces = getMaxSectionFaces(lists);
        
        if (maxSectionFaces > this.sectionFacesAllocated) {
            this.sectionFacesAllocated = Math.max(maxSectionFaces, this.sectionFacesAllocated * 2);
            this.freeCPUBuffers();
            this.allocateCPUBuffers();
        }
    
        int largestVertexIndex = 0;
        int transformsBufferPosition = transformsBufferSectionView.position();
        int indexCountsBufferPosition = 0;
        int baseVerticesBufferPosition = 0;
    
        @SuppressWarnings("unchecked")
        Collection<MdbvChunkRenderBatch>[] renderLists = new Collection[totalPasses];
        
        for (int passId = 0; passId < chunkRenderPasses.length; passId++) {
            ChunkRenderPass renderPass = chunkRenderPasses[passId];
            Deque<MdbvChunkRenderBatch> renderList = new ArrayDeque<>(128); // just an estimate, should be plenty
            
            IntList passRegionIndices = lists.regionIndices[passId];
            List<IntList> passModelPartCounts = lists.modelPartCounts[passId];
            List<LongList> passModelPartSegments = lists.modelPartSegments[passId];
            int passRegionCount = passRegionIndices.size();
    
            boolean reverseOrder = renderPass.isTranslucent();
            
            int regionIdx = reverseOrder ? passRegionCount - 1 : 0;
            while (reverseOrder ? (regionIdx >= 0) : (regionIdx < passRegionCount)) {
                IntList regionPassModelPartCounts = passModelPartCounts.get(regionIdx);
                LongList regionPassModelPartSegments = passModelPartSegments.get(regionIdx);
    
                int fullRegionIdx = passRegionIndices.getInt(regionIdx);
                RenderRegion region = lists.regions.get(fullRegionIdx);
                IntList regionSectionCoords = lists.sectionCoords.get(fullRegionIdx);
                LongList regionUploadedSegments = lists.uploadedSegments.get(fullRegionIdx);
                
                // yoink count data from the model part counts list because it doesn't scale the sizes, and it takes
                // into account the current pass
                int regionSectionCount = regionPassModelPartCounts.size();
                
                // don't use regionIdx or fullRegionIdx past here
                if (reverseOrder) {
                    regionIdx--;
                } else {
                    regionIdx++;
                }
                
                int modelPartIdx = 0;
                int sectionIdx = reverseOrder ? regionSectionCount - 1 : 0;
                while (reverseOrder ? (sectionIdx >= 0) : (sectionIdx < regionSectionCount)) {
                    int sectionModelPartCount = regionPassModelPartCounts.getInt(sectionIdx);
                    long sectionUploadedSegment = regionUploadedSegments.getLong(sectionIdx);
                    
                    int sectionCoordsIdx = sectionIdx * 3;
                    int sectionCoordX = regionSectionCoords.getInt(sectionCoordsIdx);
                    int sectionCoordY = regionSectionCoords.getInt(sectionCoordsIdx + 1);
                    int sectionCoordZ = regionSectionCoords.getInt(sectionCoordsIdx + 2);
    
                    // don't use sectionIdx past here
                    if (reverseOrder) {
                        sectionIdx--;
                    } else {
                        sectionIdx++;
                    }
                    
                    // this works because the segment is in units of vertices
                    int baseVertex = BufferSegment.getOffset(sectionUploadedSegment);
    
                    float x = getCameraTranslation(
                            ChunkSectionPos.getBlockCoord(sectionCoordX),
                            camera.blockX,
                            camera.deltaX
                    );
                    float y = getCameraTranslation(
                            ChunkSectionPos.getBlockCoord(sectionCoordY),
                            camera.blockY,
                            camera.deltaY
                    );
                    float z = getCameraTranslation(
                            ChunkSectionPos.getBlockCoord(sectionCoordZ),
                            camera.blockZ,
                            camera.deltaZ
                    );
                    
                    for (int j = 0; j < sectionModelPartCount; j++) {
                        long modelPartSegment = regionPassModelPartSegments.getLong(modelPartIdx++);
                        
                        // go from vertex count -> index count
                        MemoryUtil.memPutInt(this.indexCountsBufferPtr + indexCountsBufferPosition, 6 * (BufferSegment.getLength(modelPartSegment) >> 2));
                        indexCountsBufferPosition += Integer.BYTES;
                        MemoryUtil.memPutInt(this.baseVerticesBufferPtr + baseVerticesBufferPosition, baseVertex + BufferSegment.getOffset(modelPartSegment));
                        baseVerticesBufferPosition += Integer.BYTES;
    
                        long ptr = transformsBufferSectionAddress + transformsBufferPosition;
                        MemoryUtil.memPutFloat(ptr, x);
                        MemoryUtil.memPutFloat(ptr + 4, y);
                        MemoryUtil.memPutFloat(ptr + 8, z);
                        transformsBufferPosition += TRANSFORM_STRUCT_STRIDE;
                    }
                
                    largestVertexIndex = Math.max(largestVertexIndex, BufferSegment.getLength(sectionUploadedSegment));
                }
            
                // we have a transform for every model part, so just use the command count
                int transformsSubsectionLength = modelPartIdx * TRANSFORM_STRUCT_STRIDE;
                long transformSubsectionStart = transformsBufferSection.getDeviceOffset()
                                                + transformsBufferPosition - transformsSubsectionLength;
                transformsBufferPosition = MathUtil.align(
                        transformsBufferPosition,
                        this.uniformBufferChunkTransforms.getAlignment()
                );
                
                int indexCountsSubsectionLength = modelPartIdx * Integer.BYTES;
                long indexCountsSubsectionStart = this.indexCountsBufferPtr + indexCountsBufferPosition
                                                  - indexCountsSubsectionLength;
    
                int baseVerticesSubsectionLength = modelPartIdx * Integer.BYTES;
                long baseVerticesSubsectionStart = this.baseVerticesBufferPtr + baseVerticesBufferPosition
                                                  - baseVerticesSubsectionLength;
            
                renderList.add(new MdbvChunkRenderBatch(
                        region.getVertexBuffer().getBufferObject(),
                        region.getVertexBuffer().getStride(),
                        modelPartIdx,
                        transformSubsectionStart,
                        indexCountsSubsectionStart,
                        this.indexOffsetsBufferPtr,
                        baseVerticesSubsectionStart
                ));
            }
    
            renderLists[passId] = renderList;
        }
        
        transformsBufferSectionView.position(transformsBufferPosition);
        
        transformsBufferSection.flushPartial();
        
        this.indexBuffer.ensureCapacity(largestVertexIndex);
    
        this.renderLists = renderLists;
    }
    
    public static class MdbvChunkRenderBatch extends MdChunkRenderBatch {
        private final long indexCountsBufferPtr;
        private final long indexOffsetsBufferPtr;
        private final long baseVerticesBufferPtr;
    
        public MdbvChunkRenderBatch(
                Buffer vertexBuffer,
                int vertexStride,
                int commandCount,
                long transformBufferOffset,
                long indexCountsBufferPtr,
                long indexOffsetsBufferPtr,
                long baseVerticesBufferPtr
        ) {
            super(vertexBuffer, vertexStride, commandCount, transformBufferOffset);
            this.indexCountsBufferPtr = indexCountsBufferPtr;
            this.indexOffsetsBufferPtr = indexOffsetsBufferPtr;
            this.baseVerticesBufferPtr = baseVerticesBufferPtr;
        }
    
        public long getIndexCountsBufferPtr() {
            return this.indexCountsBufferPtr;
        }
    
        public long getIndexOffsetsBufferPtr() {
            return this.indexOffsetsBufferPtr;
        }
    
        public long getBaseVerticesBufferPtr() {
            return this.baseVerticesBufferPtr;
        }
    }
    
    protected static int unindexedTransformsBufferSize(int alignment, SortedTerrainLists list) {
        int size = 0;
        
        for (List<LongList> passModelPartSegments : list.modelPartSegments) {
            for (LongList regionModelPartSegments : passModelPartSegments) {
                size = MathUtil.align(size + (regionModelPartSegments.size() * TRANSFORM_STRUCT_STRIDE), alignment);
            }
        }
        
        return size;
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
            MdbvChunkRenderBatch batch
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
                (long) RenderRegion.REGION_SIZE * ChunkMeshFace.COUNT * TRANSFORM_STRUCT_STRIDE// the spec requires that the entire part of the UBO is filled completely, so lets just make the range the right size
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
            MdbvChunkRenderBatch batch
    ) {
        commandList.multiDrawElementsBaseVertex(
                PrimitiveType.TRIANGLES,
                ElementFormat.UNSIGNED_INT,
                batch.getCommandCount(),
                batch.getIndexCountsBufferPtr(),
                batch.getIndexOffsetsBufferPtr(),
                batch.getBaseVerticesBufferPtr()
        );
    }
    
    @Override
    public void delete() {
        super.delete();
        this.freeCPUBuffers();
    }
    
    @Override
    public String getDebugName() {
        return "MDBV";
    }
}