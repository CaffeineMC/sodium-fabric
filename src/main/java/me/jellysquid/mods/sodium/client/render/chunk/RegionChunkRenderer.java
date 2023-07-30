package me.jellysquid.mods.sodium.client.render.chunk;

import me.jellysquid.mods.sodium.client.gl.attribute.GlVertexAttributeBinding;
import me.jellysquid.mods.sodium.client.gl.device.CommandList;
import me.jellysquid.mods.sodium.client.gl.device.DrawCommandList;
import me.jellysquid.mods.sodium.client.gl.device.RenderDevice;
import me.jellysquid.mods.sodium.client.gl.tessellation.GlPrimitiveType;
import me.jellysquid.mods.sodium.client.gl.tessellation.GlTessellation;
import me.jellysquid.mods.sodium.client.gl.tessellation.TessellationBinding;
import me.jellysquid.mods.sodium.client.gl.device.MultiDrawBatch;
import me.jellysquid.mods.sodium.client.render.chunk.data.SectionRenderDataStorage;
import me.jellysquid.mods.sodium.client.render.chunk.data.SectionRenderDataUnsafe;
import me.jellysquid.mods.sodium.client.render.chunk.graph.LocalSectionIndex;
import me.jellysquid.mods.sodium.client.render.chunk.lists.SectionRenderList;
import me.jellysquid.mods.sodium.client.render.chunk.region.RenderRegionManager;
import me.jellysquid.mods.sodium.client.render.chunk.terrain.TerrainRenderPass;
import me.jellysquid.mods.sodium.client.model.quad.properties.ModelQuadFacing;
import me.jellysquid.mods.sodium.client.render.chunk.vertex.format.ChunkVertexType;
import me.jellysquid.mods.sodium.client.render.chunk.vertex.format.ChunkMeshAttribute;
import me.jellysquid.mods.sodium.client.render.chunk.region.RenderRegion;
import me.jellysquid.mods.sodium.client.render.chunk.shader.ChunkShaderBindingPoints;
import me.jellysquid.mods.sodium.client.render.chunk.shader.ChunkShaderInterface;
import me.jellysquid.mods.sodium.client.util.BitwiseMath;
import me.jellysquid.mods.sodium.client.util.ReversibleArrayIterator;
import me.jellysquid.mods.sodium.client.util.CLocalSectionListIterator;
import me.jellysquid.mods.sodium.core.types.CRegionDrawBatch;

public class RegionChunkRenderer extends ShaderChunkRenderer {
    private final MultiDrawBatch commandBufferBuilder;

    private final SharedQuadIndexBuffer sharedIndexBuffer;

    public RegionChunkRenderer(RenderDevice device, ChunkVertexType vertexType) {
        super(device, vertexType);

        this.commandBufferBuilder = new MultiDrawBatch(ModelQuadFacing.COUNT * RenderRegion.REGION_SIZE);
        this.sharedIndexBuffer = new SharedQuadIndexBuffer(device.createCommandList());
    }

    @Override
    public void render(ChunkRenderMatrices matrices, CommandList commandList,
                       SectionRenderList list, TerrainRenderPass renderPass,
                       ChunkCameraContext camera, RenderRegionManager renderRegions) {
        super.begin(renderPass);

        ChunkShaderInterface shader = this.activeProgram.getInterface();
        shader.setProjectionMatrix(matrices.projection());
        shader.setModelViewMatrix(matrices.modelView());

        ReversibleArrayIterator<CRegionDrawBatch> regionIterator = list.sortedRegions(renderPass.isReverseOrder());

        while (regionIterator.hasNext()) {
            CRegionDrawBatch regionLists = regionIterator.next();
            RenderRegion region = renderRegions.getRegionByIndex(regionLists.regionX(), regionLists.regionY(), regionLists.regionZ());

            var sections = region.getSectionStorage(renderPass);
            var resources = region.getResources();

            if (sections == null || resources == null) {
                continue;
            }

            var batch = this.prepareDrawBatch(regionLists, region, renderPass, camera, sections);

            if (batch.isEmpty()) {
                continue;
            }

            this.sharedIndexBuffer.ensureCapacity(commandList, batch.getMaxVertexCount());

            this.setModelMatrixUniforms(shader, region, camera);
            this.executeDrawBatch(commandList, batch, this.createTessellationForRegion(commandList, region));
        }

        super.end(renderPass);
    }

    private MultiDrawBatch prepareDrawBatch(CRegionDrawBatch batch, RenderRegion region, TerrainRenderPass pass, ChunkCameraContext camera, SectionRenderDataStorage sectionStorage) {
        var commandBuffer = this.commandBufferBuilder;
        commandBuffer.clear();

        CLocalSectionListIterator sectionIterator = new CLocalSectionListIterator(batch.sectionList(), pass.isReverseOrder());

        int originX = region.getChunkX();
        int originY = region.getChunkY();
        int originZ = region.getChunkZ();

        while (sectionIterator.hasNext()) {
            var localSectionIndex = sectionIterator.next();

            var pSectionData = sectionStorage.getDataPointer(localSectionIndex);

            if (SectionRenderDataUnsafe.isEmpty(pSectionData)) {
                continue;
            }

            // The position of the chunk section in world space
            var x = originX + LocalSectionIndex.unpackX(localSectionIndex);
            var y = originY + LocalSectionIndex.unpackY(localSectionIndex);
            var z = originZ + LocalSectionIndex.unpackZ(localSectionIndex);

            addDrawCalls(this.commandBufferBuilder, pSectionData,
                    getForwardFacingPlanes(camera, x, y, z));
        }

        return commandBuffer;
    }

    private static int getForwardFacingPlanes(ChunkCameraContext camera, int x, int y, int z) {
        // This is carefully written so that we can keep everything branch-less.
        //
        // Normally, this would be a ridiculous way to handle the problem. But the Hotspot VM's
        // heuristic for generating SETcc/CMOV instructions is broken, and it will always create a
        // branch even when a trivial ternary is encountered.
        //
        // For example, the following will never be transformed into a SETcc:
        //   (a > b) ? 1 : 0
        //
        // So we have to instead rely on sign-bit extension and masking (which generates a ton
        // of unnecessary instructions) to get this to be branch-less.
        //
        // To do this, we can transform the previous expression into the following.
        //   (b - a) >> 31
        //
        // This works because if (a > b) then (b - a) will always create a negative number. We then shift the sign bit
        // into the least significant bit's position (which also discards any bits following the sign bit) to get the
        // output we are looking for.
        //
        // If you look at the output which LLVM produces for a series of ternaries, you will instantly become distraught,
        // because it manages to a) correctly evaluate the cost of instructions, and b) go so far
        // as to actually produce vector code.  (https://godbolt.org/z/GaaEx39T9)

        int planes = 0;

        planes |= BitwiseMath.lessThan(x - 1, camera.chunkX) << ModelQuadFacing.POS_X;
        planes |= BitwiseMath.lessThan(y - 1, camera.chunkY) << ModelQuadFacing.POS_Y;
        planes |= BitwiseMath.lessThan(z - 1, camera.chunkZ) << ModelQuadFacing.POS_Z;

        planes |= BitwiseMath.greaterThan(x + 1, camera.chunkX) << ModelQuadFacing.NEG_X;
        planes |= BitwiseMath.greaterThan(y + 1, camera.chunkY) << ModelQuadFacing.NEG_Y;
        planes |= BitwiseMath.greaterThan(z + 1, camera.chunkZ) << ModelQuadFacing.NEG_Z;

        // the "unassigned" plane is always front-facing, since we can't check it
        planes |= (1 << ModelQuadFacing.UNASSIGNED);

        return planes;
    }

    private static void addDrawCalls(MultiDrawBatch commandBufferBuilder, long pSectionData, int visiblePlanes) {
        int baseVertex = SectionRenderDataUnsafe.getBaseVertex(pSectionData);

        // This is carefully written so that we can keep everything branch-less. Every iteration of the loop will
        // always be performed (even if the plane is not visible), but the buffer's tail pointer will only be
        // incremented when *both* the number of primitives is non-zero for a plane, and the bit index belonging to that
        // plane is marked.
        //
        // Now, some people may say that even if these branches are produced, that it just doesn't matter,
        // and that spending additional cycles is always worse than just taking the branch. There seems to be
        // a lot of misconceptions about this in general, but the important thing is that we actually measured it,
        // and observed significant improvements from moving to the branch-less implementation.
        //
        // This critical section previously consisted of ~900 instructions, of which ~45 of those were branches. The
        // captured hardware performance counters suggested that nearly half of the branches were mis-predicted, and
        // that the core was spending most of the time stalled as it tried to recover the pipeline.
        //
        // After switching to the branch-less implementation, the code was reduced to ~150 instructions and ~8 branches,
        // the latter of which are always predicted correctly since they are only assertions to prevent accidental
        // buffer overflows.
        //
        // At this point, we are mostly core bound due to the fact that we are spending so many instructions on inefficient
        // arithmetic designed to get around the compiler's aversion to producing CMOVs, but it doesn't seem like there's
        // any way to avoid that unless Hotspot gets improved.
        for (int plane = 0; plane < 7; plane++) {
            int primitiveCount = SectionRenderDataUnsafe.getBatchSize(pSectionData, plane);
            commandBufferBuilder.addConditionally(getElementsPerPrimitive(primitiveCount), baseVertex,
                    (visiblePlanes >> plane) & 1);

            baseVertex += getVerticesPerPrimitive(primitiveCount);
        }
    }

    private static int getElementsPerPrimitive(int primitiveCount) {
        return primitiveCount * 6;
    }

    private static int getVerticesPerPrimitive(int primitiveCount) {
        return primitiveCount << 2;
    }

    private GlTessellation createTessellationForRegion(CommandList commandList, RenderRegion region) {
        var resources = region.getResources();
        var tessellation = resources.getTessellation();

        if (tessellation == null) {
            resources.updateTessellation(commandList, tessellation = this.createRegionTessellation(commandList, resources));
        }

        return tessellation;
    }

    private void executeDrawBatch(CommandList commandList, MultiDrawBatch batch, GlTessellation tessellation) {
        try (DrawCommandList drawCommandList = commandList.beginTessellating(tessellation)) {
            drawCommandList.multiDrawElementsBaseVertex(batch, SharedQuadIndexBuffer.INDEX_TYPE);
        }
    }

    private void setModelMatrixUniforms(ChunkShaderInterface shader, RenderRegion region, ChunkCameraContext camera) {
        float x = getCameraTranslation(region.getWorldX(), camera.blockX, camera.deltaX);
        float y = getCameraTranslation(region.getWorldY(), camera.blockY, camera.deltaY);
        float z = getCameraTranslation(region.getWorldZ(), camera.blockZ, camera.deltaZ);

        shader.setRegionOffset(x, y, z);
    }

    private GlTessellation createRegionTessellation(CommandList commandList, RenderRegion.Resources resources) {
        return commandList.createTessellation(GlPrimitiveType.TRIANGLES, new TessellationBinding[] {
                TessellationBinding.forVertexBuffer(resources.getVertexBuffer(), new GlVertexAttributeBinding[] {
                        new GlVertexAttributeBinding(ChunkShaderBindingPoints.ATTRIBUTE_POSITION_ID,
                                this.vertexFormat.getAttribute(ChunkMeshAttribute.POSITION_MATERIAL_MESH)),
                        new GlVertexAttributeBinding(ChunkShaderBindingPoints.ATTRIBUTE_COLOR,
                                this.vertexFormat.getAttribute(ChunkMeshAttribute.COLOR_SHADE)),
                        new GlVertexAttributeBinding(ChunkShaderBindingPoints.ATTRIBUTE_BLOCK_TEXTURE,
                                this.vertexFormat.getAttribute(ChunkMeshAttribute.BLOCK_TEXTURE)),
                        new GlVertexAttributeBinding(ChunkShaderBindingPoints.ATTRIBUTE_LIGHT_TEXTURE,
                                this.vertexFormat.getAttribute(ChunkMeshAttribute.LIGHT_TEXTURE))
                }),
                TessellationBinding.forElementBuffer(this.sharedIndexBuffer.getBufferObject())
        });
    }

    @Override
    public void delete(CommandList commandList) {
        super.delete(commandList);

        this.commandBufferBuilder.delete();
        this.sharedIndexBuffer.delete(commandList);
    }

    private static float getCameraTranslation(int chunkBlockPos, int cameraBlockPos, float cameraPos) {
        return (chunkBlockPos - cameraBlockPos) - cameraPos;
    }
}
