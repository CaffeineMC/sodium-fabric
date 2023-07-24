package me.jellysquid.mods.sodium.client.render.chunk;

import me.jellysquid.mods.sodium.client.SodiumClientMod;
import me.jellysquid.mods.sodium.client.gl.attribute.GlVertexAttributeBinding;
import me.jellysquid.mods.sodium.client.gl.device.CommandList;
import me.jellysquid.mods.sodium.client.gl.device.DrawCommandList;
import me.jellysquid.mods.sodium.client.gl.device.RenderDevice;
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
import me.jellysquid.mods.sodium.client.util.ReversibleArrayIterator;

import java.util.EnumMap;

public class RegionChunkRenderer extends ShaderChunkRenderer {
    private final MultiDrawBatch commandBufferBuilder;
    private final boolean isBlockFaceCullingEnabled = SodiumClientMod.options().performance.useBlockFaceCulling;

    private final EnumMap<SharedQuadIndexBuffer.IndexType, SharedQuadIndexBuffer> sharedIndexBuffers = new EnumMap<>(SharedQuadIndexBuffer.IndexType.class);

    public RegionChunkRenderer(RenderDevice device, ChunkVertexType vertexType) {
        super(device, vertexType);

        this.commandBufferBuilder = new MultiDrawBatch(ModelQuadFacing.COUNT * RenderRegion.REGION_SIZE);

        for (var indexType : SharedQuadIndexBuffer.IndexType.values()) {
            this.sharedIndexBuffers.put(indexType, new SharedQuadIndexBuffer(device.createCommandList(), indexType));
        }
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

            this.prepareDrawBatch(commandBufferBuilder, regionStorage, renderList, renderPass, camera);

            if (commandBufferBuilder.isEmpty()) {
                continue;
            }

            var maxVertexCount = commandBufferBuilder.getMaxVertexCount();
            var indexType = SharedQuadIndexBuffer.getSmallestIndexType(maxVertexCount);

            var indexBuffer = this.sharedIndexBuffers.get(indexType);
            indexBuffer.ensureCapacity(commandList, maxVertexCount);

            var tessellation = this.createTessellationForRegion(commandList, region, indexBuffer);

            this.setModelMatrixUniforms(shader, region, camera);
            this.executeDrawBatch(commandList, commandBufferBuilder, indexBuffer, tessellation);
        }

        super.end(renderPass);
    }

    private void prepareDrawBatch(MultiDrawBatch commandBufferBuilder, RenderRegion.SectionStorage storage, ChunkRenderList renderList, TerrainRenderPass pass, ChunkCameraContext camera) {
        var sectionIterator = renderList.sectionsWithGeometryIterator(pass.isReverseOrder());

        if (sectionIterator == null) {
            return;
        }

        while (sectionIterator.hasNext()) {
            var sectionId = sectionIterator.next();
            var sectionRenderData = storage.getRenderData(sectionId);

            if (sectionRenderData == null) {
                continue;
            }

            this.collectDrawCommands(commandBufferBuilder, sectionRenderData, camera);
        }
    }

    private void collectDrawCommands(MultiDrawBatch commandBufferBuilder, ChunkGraphicsState renderData, ChunkCameraContext camera) {
        if (this.isBlockFaceCullingEnabled) {
            this.addFilteredDrawCommands(commandBufferBuilder, renderData, camera);
        } else {
            this.addUnfilteredDrawCommands(commandBufferBuilder, renderData);
        }
    }

    private void addFilteredDrawCommands(MultiDrawBatch commandBufferBuilder, ChunkGraphicsState renderData, ChunkCameraContext camera) {
        int faces = this.getFrontFacingPlanes(camera, renderData) & renderData.getNonEmptyModelParts();

        if (faces == 0) {
            return;
        }

        final int[] slices = renderData.getModelParts();

        int base = renderData.getBaseVertex();
        int offset = 0;

        for (int facing = 0; facing < ModelQuadFacing.COUNT; facing++) {
            int count = slices[facing];

            if ((faces & (1 << facing)) != 0) {
                addDrawCommand(commandBufferBuilder, offset, count, base);
            }

            offset += count;
        }
    }

    private void addUnfilteredDrawCommands(MultiDrawBatch commandBufferBuilder, ChunkGraphicsState renderData) {
        if (renderData.getVertexCount() > 0) {
            addDrawCommand(commandBufferBuilder, 0, renderData.getVertexCount(), renderData.getBaseVertex());
        }
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

    private int getFrontFacingPlanes(ChunkCameraContext camera, ChunkGraphicsState renderData) {
        int flags = 0;

        // Always added, as we can't determine whether these faces are visible
        flags |= 1 << MODEL_UNASSIGNED;

        if (camera.blockX > (renderData.getMinX() - RENDER_BOUNDS_MARGIN)) {
            flags |= 1 << MODEL_POS_X;
        }

        if (camera.blockX < (renderData.getMaxX() + RENDER_BOUNDS_MARGIN)) {
            flags |= 1 << MODEL_NEG_X;
        }

        if (camera.blockY > (renderData.getMinY() - RENDER_BOUNDS_MARGIN)) {
            flags |= 1 << MODEL_POS_Y;
        }

        if (camera.blockY < (renderData.getMaxY() + RENDER_BOUNDS_MARGIN)) {
            flags |= 1 << MODEL_NEG_Y;
        }

        if (camera.blockZ > (renderData.getMinZ() - RENDER_BOUNDS_MARGIN)) {
            flags |= 1 << MODEL_POS_Z;
        }

        if (camera.blockZ < (renderData.getMaxZ() + RENDER_BOUNDS_MARGIN)) {
            flags |= 1 << MODEL_NEG_Z;
        }

        return flags;
    }

    private GlTessellation createTessellationForRegion(CommandList commandList, RenderRegion region, SharedQuadIndexBuffer indexBuffer) {
        var indexType = indexBuffer.getIndexType();

        var resources = region.getResources();
        var tessellation = resources.getTessellation(indexType);

        if (tessellation == null) {
            resources.updateTessellation(commandList, indexType, tessellation = this.createRegionTessellation(commandList, resources, indexBuffer));
        }

        return tessellation;
    }

    private void executeDrawBatch(CommandList commandList, MultiDrawBatch batch, SharedQuadIndexBuffer indexBuffer, GlTessellation tessellation) {
        try (DrawCommandList drawCommandList = commandList.beginTessellating(tessellation)) {
            drawCommandList.multiDrawElementsBaseVertex(batch, indexBuffer.getIndexFormat());
        }
    }

    private void setModelMatrixUniforms(ChunkShaderInterface shader, RenderRegion region, ChunkCameraContext camera) {
        float x = getCameraTranslation(region.getOriginX(), camera.blockX, camera.deltaX);
        float y = getCameraTranslation(region.getOriginY(), camera.blockY, camera.deltaY);
        float z = getCameraTranslation(region.getOriginZ(), camera.blockZ, camera.deltaZ);

        shader.setRegionOffset(x, y, z);
    }

    private static void addDrawCommand(MultiDrawBatch batch, int vertexStart, int vertexCount, int bufferBase) {
        batch.add(0L, (vertexCount >> 2) * 6, bufferBase + vertexStart);
    }

    private GlTessellation createRegionTessellation(CommandList commandList, RenderRegion.DeviceResources resources, SharedQuadIndexBuffer indexBuffer) {
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
                TessellationBinding.forElementBuffer(indexBuffer.getBufferObject())
        });
    }

    @Override
    public void delete(CommandList commandList) {
        super.delete(commandList);

        this.commandBufferBuilder.delete();

        for (var indexBuffer : this.sharedIndexBuffers.values()) {
            indexBuffer.delete(commandList);
        }
    }

    private static float getCameraTranslation(int chunkBlockPos, int cameraBlockPos, float cameraPos) {
        return (chunkBlockPos - cameraBlockPos) - cameraPos;
    }
}
