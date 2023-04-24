package me.jellysquid.mods.sodium.client.render.chunk;

import me.jellysquid.mods.sodium.client.SodiumClientMod;
import me.jellysquid.mods.sodium.client.gl.attribute.GlVertexAttributeBinding;
import me.jellysquid.mods.sodium.client.gl.device.CommandList;
import me.jellysquid.mods.sodium.client.gl.device.DrawCommandList;
import me.jellysquid.mods.sodium.client.gl.device.RenderDevice;
import me.jellysquid.mods.sodium.client.gl.tessellation.GlPrimitiveType;
import me.jellysquid.mods.sodium.client.gl.tessellation.GlTessellation;
import me.jellysquid.mods.sodium.client.gl.tessellation.TessellationBinding;
import me.jellysquid.mods.sodium.client.gl.util.VertexRange;
import me.jellysquid.mods.sodium.client.gl.device.MultiDrawBatch;
import me.jellysquid.mods.sodium.client.render.chunk.lists.ChunkRenderList;
import me.jellysquid.mods.sodium.client.render.chunk.terrain.TerrainRenderPass;
import me.jellysquid.mods.sodium.client.model.quad.properties.ModelQuadFacing;
import me.jellysquid.mods.sodium.client.render.chunk.region.RenderRegionManager;
import me.jellysquid.mods.sodium.client.render.chunk.vertex.format.ChunkVertexType;
import me.jellysquid.mods.sodium.client.render.chunk.data.ChunkRenderBounds;
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
                       RenderRegionManager regions, ChunkRenderList list, TerrainRenderPass renderPass,
                       ChunkCameraContext camera) {
        super.begin(renderPass);

        ChunkShaderInterface shader = this.activeProgram.getInterface();
        shader.setProjectionMatrix(matrices.projection());
        shader.setModelViewMatrix(matrices.modelView());

        ReversibleArrayIterator<ChunkRenderList.RegionBatch> regionIterator = list.batches(renderPass.isReverseOrder());
        ChunkRenderList.RegionBatch regionBatch;

        while ((regionBatch = regionIterator.next()) != null) {
            var region = regions.getRegion(regionBatch.getRegionId());
            var regionStorage = region.getStorage(renderPass);

            if (regionStorage == null) {
                continue;
            }

            var batch = this.prepareDrawBatch(regionStorage, regionBatch, renderPass, camera);

            if (batch.isEmpty()) {
                continue;
            }

            var indexType = SharedQuadIndexBuffer.getSmallestIndexType(batch.getMaxVertexCount());

            var indexBuffer = this.sharedIndexBuffers.get(indexType);
            indexBuffer.ensureCapacity(commandList, batch.getMaxVertexCount());

            this.setModelMatrixUniforms(shader, region, camera);
            this.executeDrawBatch(commandList, batch, indexBuffer, this.createTessellationForRegion(commandList, region, renderPass, indexBuffer));
        }

        super.end(renderPass);
    }

    private MultiDrawBatch prepareDrawBatch(RenderRegion.RenderRegionStorage storage, ChunkRenderList.RegionBatch batch, TerrainRenderPass pass, ChunkCameraContext camera) {
        var commandBuffer = this.commandBufferBuilder;
        commandBuffer.clear();

        ReversibleArrayIterator<RenderSection> sectionIterator = batch.ordered(pass.isReverseOrder());
        RenderSection section;

        while ((section = sectionIterator.next()) != null) {
            ChunkGraphicsState state = storage.getState(section);

            if (state == null) {
                continue;
            }

            int baseVertex = state.getVertexSegment()
                    .getOffset();

            this.addDrawCalls(camera, section, state, baseVertex);
        }

        return commandBuffer;
    }

    private void addDrawCalls(ChunkCameraContext camera, RenderSection section, ChunkGraphicsState state, int baseVertex) {
        var commandBufferBuilder = this.commandBufferBuilder;

        addDrawCall(commandBufferBuilder, state.getModelPart(ModelQuadFacing.UNASSIGNED), baseVertex);

        if (this.isBlockFaceCullingEnabled) {
            ChunkRenderBounds bounds = section.getBounds();

            if (camera.posY > bounds.minY) {
                addDrawCall(commandBufferBuilder, state.getModelPart(ModelQuadFacing.UP), baseVertex);
            }

            if (camera.posY < bounds.maxY) {
                addDrawCall(commandBufferBuilder, state.getModelPart(ModelQuadFacing.DOWN), baseVertex);
            }

            if (camera.posX > bounds.minX) {
                addDrawCall(commandBufferBuilder, state.getModelPart(ModelQuadFacing.EAST), baseVertex);
            }

            if (camera.posX < bounds.maxX) {
                addDrawCall(commandBufferBuilder, state.getModelPart(ModelQuadFacing.WEST), baseVertex);
            }

            if (camera.posZ > bounds.minZ) {
                addDrawCall(commandBufferBuilder, state.getModelPart(ModelQuadFacing.SOUTH), baseVertex);
            }

            if (camera.posZ < bounds.maxZ) {
                addDrawCall(commandBufferBuilder, state.getModelPart(ModelQuadFacing.NORTH), baseVertex);
            }
        } else {
            for (ModelQuadFacing facing : ModelQuadFacing.DIRECTIONS) {
                addDrawCall(commandBufferBuilder, state.getModelPart(facing), baseVertex);
            }
        }
    }

    private GlTessellation createTessellationForRegion(CommandList commandList, RenderRegion region, TerrainRenderPass pass, SharedQuadIndexBuffer indexBuffer) {
        var indexType = indexBuffer.getIndexType();

        var storage = region.getStorage(pass);
        var tessellation = storage.getTessellation(indexType);

        if (tessellation == null) {
            storage.updateTessellation(commandList, indexType, tessellation = this.createRegionTessellation(commandList, region, indexBuffer));
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

    private static void addDrawCall(MultiDrawBatch batch, VertexRange part, int baseVertex) {
        if (part != null) {
            batch.add(0L, (part.vertexCount() >> 2) * 6, baseVertex + part.vertexStart());
        }
    }

    private GlTessellation createRegionTessellation(CommandList commandList, RenderRegion region, SharedQuadIndexBuffer indexBuffer) {
        return commandList.createTessellation(GlPrimitiveType.TRIANGLES, new TessellationBinding[] {
                TessellationBinding.forVertexBuffer(region.vertexBuffers.getBufferObject(), new GlVertexAttributeBinding[] {
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
