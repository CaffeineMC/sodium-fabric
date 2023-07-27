package me.jellysquid.mods.sodium.client.render.chunk;

import me.jellysquid.mods.sodium.client.gl.attribute.GlVertexAttributeBinding;
import me.jellysquid.mods.sodium.client.gl.device.CommandList;
import me.jellysquid.mods.sodium.client.gl.device.DrawCommandList;
import me.jellysquid.mods.sodium.client.gl.device.MultiDrawBatch;
import me.jellysquid.mods.sodium.client.gl.device.RenderDevice;
import me.jellysquid.mods.sodium.client.gl.tessellation.GlIndexType;
import me.jellysquid.mods.sodium.client.gl.tessellation.GlPrimitiveType;
import me.jellysquid.mods.sodium.client.gl.tessellation.GlTessellation;
import me.jellysquid.mods.sodium.client.gl.tessellation.TessellationBinding;
import me.jellysquid.mods.sodium.client.model.quad.properties.ModelQuadFacing;
import me.jellysquid.mods.sodium.client.render.chunk.data.SectionRenderDataStorage;
import me.jellysquid.mods.sodium.client.render.chunk.data.SectionRenderDataUnsafe;
import me.jellysquid.mods.sodium.client.render.chunk.lists.ChunkRenderList;
import me.jellysquid.mods.sodium.client.render.chunk.lists.SortedRenderLists;
import me.jellysquid.mods.sodium.client.render.chunk.region.RenderRegion;
import me.jellysquid.mods.sodium.client.render.chunk.region.RenderRegionManager;
import me.jellysquid.mods.sodium.client.render.chunk.shader.ChunkShaderBindingPoints;
import me.jellysquid.mods.sodium.client.render.chunk.shader.ChunkShaderInterface;
import me.jellysquid.mods.sodium.client.render.chunk.terrain.TerrainRenderPass;
import me.jellysquid.mods.sodium.client.render.chunk.vertex.format.ChunkMeshAttribute;
import me.jellysquid.mods.sodium.client.render.chunk.vertex.format.ChunkVertexType;
import me.jellysquid.mods.sodium.client.util.ReversibleArrayIterator;
import org.lwjgl.system.MemoryUtil;

public class RegionChunkRenderer extends ShaderChunkRenderer {
    private final MultiDrawBatch batch;

    private final SharedQuadIndexBuffer sharedIndexBuffer;

    public RegionChunkRenderer(RenderDevice device, ChunkVertexType vertexType) {
        super(device, vertexType);

        this.batch = new MultiDrawBatch((ModelQuadFacing.COUNT * RenderRegion.REGION_SIZE) + 1);
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
            var storage = region.getStorage(renderPass);

            if (storage == null) {
                continue;
            }

            fillCommandBuffer(this.batch, storage, renderList, renderPass);

            if (this.batch.isEmpty()) {
                continue;
            }

            this.sharedIndexBuffer.ensureCapacity(commandList, this.batch.getIndexBufferSize());

            var tessellation = this.prepareTessellation(commandList, region);

            setModelMatrixUniforms(shader, region, camera);
            executeDrawBatch(commandList, tessellation, this.batch);
        }

        super.end(renderPass);
    }

    private static void fillCommandBuffer(MultiDrawBatch batch, SectionRenderDataStorage storage, ChunkRenderList list, TerrainRenderPass pass) {
        batch.clear();

        var sectionIterator = list.sectionsWithGeometryIterator(pass.isReverseOrder());

        if (sectionIterator == null) {
            return;
        }

        while (sectionIterator.hasNext()) {
            var next = sectionIterator.next();

            var sectionIndex = ChunkRenderList.unpackIndex(next);
            var sectionCulledFaces = ChunkRenderList.unpackFaces(next);

            var data = storage.getDataPointer(sectionIndex);

            int slices = sectionCulledFaces;
            slices &= SectionRenderDataUnsafe.getSliceMask(data);

            if (slices != 0) {
                addDrawCommands(batch, data, slices);
            }
        }
    }

    @SuppressWarnings("IntegerMultiplicationImplicitCastToLong")
    private static void addDrawCommands(MultiDrawBatch batch, long data, int mask) {
        final var pBaseVertex = batch.pBaseVertex;
        final var pElementCount = batch.pElementCount;

        int size = batch.size;

        for (int facing = 0; facing < ModelQuadFacing.COUNT; facing++) {
            MemoryUtil.memPutInt(pBaseVertex + (size << 2), SectionRenderDataUnsafe.getVertexOffset(data, facing));
            MemoryUtil.memPutInt(pElementCount + (size << 2), SectionRenderDataUnsafe.getElementCount(data, facing));

            size += (mask >> facing) & 1;
        }

        batch.size = size;
    }

    private static void setModelMatrixUniforms(ChunkShaderInterface shader, RenderRegion region, ChunkCameraContext camera) {
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

    private static void executeDrawBatch(CommandList commandList, GlTessellation tessellation, MultiDrawBatch batch) {
        try (DrawCommandList drawCommandList = commandList.beginTessellating(tessellation)) {
            drawCommandList.multiDrawElementsBaseVertex(batch, GlIndexType.UNSIGNED_INT);
        }
    }

    @Override
    public void delete(CommandList commandList) {
        super.delete(commandList);

        this.sharedIndexBuffer.delete(commandList);
        this.batch.delete();
    }
}
