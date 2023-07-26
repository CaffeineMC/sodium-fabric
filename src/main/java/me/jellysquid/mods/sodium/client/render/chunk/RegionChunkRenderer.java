package me.jellysquid.mods.sodium.client.render.chunk;

import me.jellysquid.mods.sodium.client.SodiumClientMod;
import me.jellysquid.mods.sodium.client.gl.attribute.GlVertexAttributeBinding;
import me.jellysquid.mods.sodium.client.gl.device.CommandList;
import me.jellysquid.mods.sodium.client.gl.device.DrawCommandList;
import me.jellysquid.mods.sodium.client.gl.device.RenderDevice;
import me.jellysquid.mods.sodium.client.gl.tessellation.GlIndexType;
import me.jellysquid.mods.sodium.client.gl.tessellation.GlPrimitiveType;
import me.jellysquid.mods.sodium.client.gl.tessellation.GlTessellation;
import me.jellysquid.mods.sodium.client.gl.tessellation.TessellationBinding;
import me.jellysquid.mods.sodium.client.gl.device.MultiDrawBatch;
import me.jellysquid.mods.sodium.client.render.chunk.lists.ChunkRenderList;
import me.jellysquid.mods.sodium.client.render.chunk.lists.SortedRenderLists;
import me.jellysquid.mods.sodium.client.render.chunk.terrain.TerrainRenderPass;
import me.jellysquid.mods.sodium.client.model.quad.properties.ModelQuadFacing;
import me.jellysquid.mods.sodium.client.render.chunk.region.RenderRegionManager;
import me.jellysquid.mods.sodium.client.render.chunk.vertex.format.ChunkVertexType;
import me.jellysquid.mods.sodium.client.render.chunk.vertex.format.ChunkMeshAttribute;
import me.jellysquid.mods.sodium.client.render.chunk.region.RenderRegion;
import me.jellysquid.mods.sodium.client.render.chunk.shader.ChunkShaderBindingPoints;
import me.jellysquid.mods.sodium.client.render.chunk.shader.ChunkShaderInterface;
import me.jellysquid.mods.sodium.client.util.BitwiseMath;
import me.jellysquid.mods.sodium.client.util.ReversibleArrayIterator;

public class RegionChunkRenderer extends ShaderChunkRenderer {
    private final MultiDrawBatch commandBufferBuilder;
    private final boolean isBlockFaceCullingEnabled = SodiumClientMod.options().performance.useBlockFaceCulling;

    private final SharedQuadIndexBuffer sharedIndexBuffer;

    public RegionChunkRenderer(RenderDevice device, ChunkVertexType vertexType) {
        super(device, vertexType);

        this.commandBufferBuilder = new MultiDrawBatch(ModelQuadFacing.COUNT * RenderRegion.REGION_SIZE);
        this.sharedIndexBuffer = new SharedQuadIndexBuffer(device.createCommandList(), SharedQuadIndexBuffer.IndexType.INTEGER);
    }

    @Override
    public void render(ChunkRenderMatrices matrices, CommandList commandList,
                       RenderRegionManager regions, SortedRenderLists renderLists, TerrainRenderPass renderPass,
                       ChunkCameraContext camera) {
        super.begin(renderPass);

        ChunkShaderInterface shader = this.activeProgram.getInterface();
        shader.setProjectionMatrix(matrices.projection());
        shader.setModelViewMatrix(matrices.modelView());

        ReversibleArrayIterator<ChunkRenderList> regionIterator = renderLists.sorted(renderPass.isReverseOrder());
        ChunkRenderList renderList;

        while ((renderList = regionIterator.next()) != null) {
            var region = renderList.getRegion();
            var regionStorage = region.getStorage(renderPass);

            if (regionStorage == null) {
                continue;
            }

            var commandBufferBuilder = this.commandBufferBuilder;
            commandBufferBuilder.clear();

            int indexBufferSize = this.prepareDrawBatch(commandBufferBuilder, regionStorage, renderList, renderPass, camera);

            if (commandBufferBuilder.isEmpty()) {
                continue;
            }

            this.sharedIndexBuffer.ensureCapacity(commandList, indexBufferSize);

            this.setModelMatrixUniforms(shader, region, camera);

            this.executeDrawBatch(commandList, commandBufferBuilder, this.prepareTessellation(commandList, region));
        }

        super.end(renderPass);
    }

    private int prepareDrawBatch(MultiDrawBatch commandBufferBuilder, RenderRegion.SectionStorage storage, ChunkRenderList renderList, TerrainRenderPass pass, ChunkCameraContext camera) {
        var sectionIterator = renderList.sectionsWithGeometryIterator(pass.isReverseOrder());

        if (sectionIterator == null) {
            return 0;
        }

        int indexBufferSize = 0;

        while (sectionIterator.hasNext()) {
            var sectionId = sectionIterator.next();
            var sectionRenderData = storage.getRenderData(sectionId);

            if (sectionRenderData == null) {
                continue;
            }

            this.collectDrawCommands(commandBufferBuilder, sectionRenderData, camera);

            indexBufferSize = Math.max(indexBufferSize, sectionRenderData.getVertexCount());
        }

        return indexBufferSize;
    }

    private void collectDrawCommands(MultiDrawBatch batch, ChunkGraphicsState data, ChunkCameraContext camera) {
        int slices;

        if (this.isBlockFaceCullingEnabled) {
            slices = getVisibleSlices(camera, data);
        } else {
            slices = 0b1111111;
        }

        slices &= data.getSliceMask();

        if (slices == 0b1111111) {
            this.addUnfilteredDrawCommands(batch, data);
        } else if (slices != 0) {
            this.addFilteredDrawCommands(batch, data, slices);
        }
    }

    private void addFilteredDrawCommands(MultiDrawBatch batch, ChunkGraphicsState data, int slices) {
        int elementOffset = 0;

        for (int facing = 0; facing < ModelQuadFacing.COUNT; facing++) {
            int elementCount = data.getSliceRange(facing);

            if ((slices & (1 << facing)) != 0) {
                addDrawCommand(batch, elementOffset, elementCount, data.getBaseVertex());
            }

            elementOffset += elementCount;
        }
    }

    private void addUnfilteredDrawCommands(MultiDrawBatch batch, ChunkGraphicsState renderData) {
        if (renderData.getVertexCount() > 0) {
            addDrawCommand(batch, 0, (renderData.getVertexCount() >> 2) * 6, renderData.getBaseVertex());
        }
    }

    private static void addDrawCommand(MultiDrawBatch batch, int elementOffset, int elementCount, int baseVertex) {
        batch.add(elementOffset * 4L, elementCount, baseVertex);
    }

    /**
     * The number of blocks to extend the render bounds of a chunk section. Since block models can emit geometry
     * which is outside the section's bounds, we need some margin.
     */
    private static final int RENDER_BOUNDS_MARGIN = 4;

    private static final int MODEL_UNASSIGNED = ModelQuadFacing.UNASSIGNED.ordinal();
    private static final int MODEL_POS_X      = ModelQuadFacing.EAST.ordinal();
    private static final int MODEL_NEG_X      = ModelQuadFacing.WEST.ordinal();
    private static final int MODEL_POS_Y      = ModelQuadFacing.UP.ordinal();
    private static final int MODEL_NEG_Y      = ModelQuadFacing.DOWN.ordinal();
    private static final int MODEL_POS_Z      = ModelQuadFacing.SOUTH.ordinal();
    private static final int MODEL_NEG_Z      = ModelQuadFacing.NORTH.ordinal();

    private static int getVisibleSlices(ChunkCameraContext camera, ChunkGraphicsState renderData) {
        int flags = 0;

        // Always added, as we can't determine whether these faces are visible
        flags |= 1 << MODEL_UNASSIGNED;

        // Use bitwise math to avoid branches created by the JIT compiler
        flags |= BitwiseMath.greaterThan(camera.blockX, (renderData.getMinX() - RENDER_BOUNDS_MARGIN)) << MODEL_POS_X;
        flags |= BitwiseMath.greaterThan(camera.blockY, (renderData.getMinY() - RENDER_BOUNDS_MARGIN)) << MODEL_POS_Y;
        flags |= BitwiseMath.greaterThan(camera.blockZ, (renderData.getMinZ() - RENDER_BOUNDS_MARGIN)) << MODEL_POS_Z;

        flags |= BitwiseMath.lessThan(camera.blockX, (renderData.getMaxX() + RENDER_BOUNDS_MARGIN)) << MODEL_NEG_X;
        flags |= BitwiseMath.lessThan(camera.blockY, (renderData.getMaxY() + RENDER_BOUNDS_MARGIN)) << MODEL_NEG_Y;
        flags |= BitwiseMath.lessThan(camera.blockZ, (renderData.getMaxZ() + RENDER_BOUNDS_MARGIN)) << MODEL_NEG_Z;

        return flags;
    }

    private void setModelMatrixUniforms(ChunkShaderInterface shader, RenderRegion region, ChunkCameraContext camera) {
        float x = getCameraTranslation(region.getOriginX(), camera.blockX, camera.deltaX);
        float y = getCameraTranslation(region.getOriginY(), camera.blockY, camera.deltaY);
        float z = getCameraTranslation(region.getOriginZ(), camera.blockZ, camera.deltaZ);

        shader.setRegionOffset(x, y, z);
    }

    private static float getCameraTranslation(int chunkBlockPos, int cameraBlockPos, float cameraPos) {
        return (chunkBlockPos - cameraBlockPos) - cameraPos;
    }

    private GlTessellation prepareTessellation(CommandList commandList, RenderRegion region) {
        var resources = region.getResources();
        var tessellation = resources.getTessellation();

        if (tessellation == null) {
            resources.updateTessellation(commandList, tessellation = this.createRegionTessellation(commandList, resources));
        }

        return tessellation;
    }

    private GlTessellation createRegionTessellation(CommandList commandList, RenderRegion.DeviceResources resources) {
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

    private void executeDrawBatch(CommandList commandList, MultiDrawBatch batch, GlTessellation tessellation) {
        try (DrawCommandList drawCommandList = commandList.beginTessellating(tessellation)) {
            drawCommandList.multiDrawElementsBaseVertex(batch, GlIndexType.UNSIGNED_INT);
        }
    }

    @Override
    public void delete(CommandList commandList) {
        super.delete(commandList);

        this.sharedIndexBuffer.delete(commandList);
        this.commandBufferBuilder.delete();
    }
}
