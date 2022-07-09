package net.caffeinemc.sodium.render.chunk.draw;

import java.nio.ByteBuffer;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.Iterator;
import net.caffeinemc.gfx.api.buffer.Buffer;
import net.caffeinemc.gfx.api.device.RenderDevice;
import net.caffeinemc.gfx.api.device.commands.RenderCommandList;
import net.caffeinemc.gfx.api.pipeline.Pipeline;
import net.caffeinemc.gfx.api.pipeline.PipelineState;
import net.caffeinemc.gfx.api.types.ElementFormat;
import net.caffeinemc.gfx.api.types.PrimitiveType;
import net.caffeinemc.gfx.util.buffer.StreamingBuffer;
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
    public void createRenderLists(SortedChunkLists chunks, ChunkCameraContext camera, int frameIndex) {
        if (chunks.isEmpty()) {
            this.renderLists = null;
            return;
        }
    
        ChunkRenderPass[] chunkRenderPasses = this.renderPassManager.getAllRenderPasses();
        int totalPasses = chunkRenderPasses.length;
        
        boolean useBlockFaceCulling = SodiumClientMod.options().performance.useBlockFaceCulling;
    
        // setup buffers, resizing as needed
        int transformsBufferPassSize = unindexedTransformsBufferSize(this.uniformBufferChunkTransforms.getAlignment(), chunks);
        StreamingBuffer.WritableSection transformsBufferSection = this.uniformBufferChunkTransforms.getSection(
                frameIndex,
                transformsBufferPassSize,
                false
        );
        ByteBuffer transformsBufferSectionView = transformsBufferSection.getView();
        long transformsBufferSectionAddress = MemoryUtil.memAddress0(transformsBufferSectionView);
        
        int maxSectionFaces = getMaxSectionFaces(chunks);
        
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
            Deque<MdbvChunkRenderBatch> renderList = new ArrayDeque<>(16); // just an estimate
    
            boolean reverseOrder = renderPass.isTranslucent();
        
            for (Iterator<SortedChunkLists.RegionBucket> regionIterator = chunks.sortedRegionBuckets(reverseOrder); regionIterator.hasNext(); ) {
                SortedChunkLists.RegionBucket regionBucket = regionIterator.next();
                
                int batchCommandCount = 0;
            
                for (Iterator<RenderSection> sectionIterator = regionBucket.sortedSections(reverseOrder); sectionIterator.hasNext(); ) {
                    RenderSection section = sectionIterator.next();
    
                    BufferSegment uploadedSegment = section.getUploadedGeometrySegment();
                    
                    if (uploadedSegment == null) {
                        continue;
                    }
                    
                    ChunkPassModel[] models = section.getData().models;
                
                    int baseVertex = uploadedSegment.getOffset();
                
                    int visibility = calculateVisibilityFlags(section.getData().bounds, camera);
                
                    ChunkPassModel model = models[passId];
                
                    if (model == null || (model.getVisibilityBits() & visibility) == 0) {
                        continue;
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
                
                    ModelRange[] modelParts = model.getModelParts();
                    for (int dir = 0; dir < modelParts.length; dir++) {
                        if (useBlockFaceCulling && (visibility & (1 << dir)) == 0) {
                            continue;
                        }
                    
                        ModelRange modelPart = modelParts[dir];
                    
                        if (modelPart == null) {
                            continue;
                        }
                        
                        MemoryUtil.memPutInt(this.indexCountsBufferPtr + indexCountsBufferPosition, modelPart.indexCount());
                        indexCountsBufferPosition += Integer.BYTES;
                        MemoryUtil.memPutInt(this.baseVerticesBufferPtr + baseVerticesBufferPosition, baseVertex + modelPart.firstVertex());
                        baseVerticesBufferPosition += Integer.BYTES;
    
                        long ptr = transformsBufferSectionAddress + transformsBufferPosition;
                        MemoryUtil.memPutFloat(ptr + 0, x);
                        MemoryUtil.memPutFloat(ptr + 4, y);
                        MemoryUtil.memPutFloat(ptr + 8, z);
                        transformsBufferPosition += TRANSFORM_STRUCT_STRIDE;
                        
                        batchCommandCount++;
                    }
                
                    largestVertexIndex = Math.max(largestVertexIndex, uploadedSegment.getLength());
                }
            
                if (batchCommandCount == 0) {
                    continue;
                }
            
                // we have a transform for every command, so just use the command count
                int transformsSubsectionLength = batchCommandCount * TRANSFORM_STRUCT_STRIDE;
                long transformSubsectionStart = transformsBufferSection.getDeviceOffset()
                                                + transformsBufferPosition - transformsSubsectionLength;
                transformsBufferPosition = MathUtil.align(
                        transformsBufferPosition,
                        this.uniformBufferChunkTransforms.getAlignment()
                );
                
                int indexCountsSubsectionLength = batchCommandCount * Integer.BYTES;
                long indexCountsSubsectionStart = this.indexCountsBufferPtr + indexCountsBufferPosition
                                                  - indexCountsSubsectionLength;
    
                int baseVerticesSubsectionLength = batchCommandCount * Integer.BYTES;
                long baseVerticesSubsectionStart = this.baseVerticesBufferPtr + baseVerticesBufferPosition
                                                  - baseVerticesSubsectionLength;
            
                RenderRegion region = regionBucket.getRegion();
            
                renderList.add(new MdbvChunkRenderBatch(
                        region.vertexBuffers.getBufferObject(),
                        region.vertexBuffers.getStride(),
                        batchCommandCount,
                        transformSubsectionStart,
                        indexCountsSubsectionStart,
                        this.indexOffsetsBufferPtr,
                        baseVerticesSubsectionStart
                ));
            }
        
            if (!renderList.isEmpty()) {
                renderLists[passId] = renderList;
            }
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
    
    protected static int unindexedTransformsBufferSize(int alignment, SortedChunkLists list) {
        int size = 0;
    
        for (SortedChunkLists.RegionBucket regionBucket : list.unsortedRegionBuckets()) {
            for (RenderSection section : regionBucket.unsortedSections()) {
                for (ChunkPassModel model : section.getData().models) {
                    // each bit set represents a model, so we can just count the set bits
                    size += Integer.bitCount(model.getVisibilityBits()) * TRANSFORM_STRUCT_STRIDE;
                }
            }
            size = MathUtil.align(size, alignment);
        }
        
        return size;
    }
    
    @Override
    protected void setupPerBatch(
            ChunkRenderPass renderPass,
            ChunkRenderMatrices matrices,
            int frameIndex,
            Pipeline<ChunkShaderInterface, BufferTarget> pipeline,
            RenderCommandList<BufferTarget> commandList,
            ChunkShaderInterface programInterface,
            PipelineState pipelineState,
            MdbvChunkRenderBatch batch
    ) {
        super.setupPerBatch(
                renderPass,
                matrices,
                frameIndex,
                pipeline,
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
            Pipeline<ChunkShaderInterface, BufferTarget> pipeline,
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