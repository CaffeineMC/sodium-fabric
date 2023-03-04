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
import me.jellysquid.mods.sodium.client.gl.util.ElementRange;
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

public class RegionChunkRenderer extends ShaderChunkRenderer {
    private final MultiDrawBatch commandBufferBuilder;
    private final GlVertexAttributeBinding[] vertexAttributeBindings;

    private final boolean isBlockFaceCullingEnabled = SodiumClientMod.options().performance.useBlockFaceCulling;

    public RegionChunkRenderer(RenderDevice device, ChunkVertexType vertexType) {
        super(device, vertexType);

        this.vertexAttributeBindings = new GlVertexAttributeBinding[] {
                new GlVertexAttributeBinding(ChunkShaderBindingPoints.ATTRIBUTE_POSITION_ID,
                        this.vertexFormat.getAttribute(ChunkMeshAttribute.POSITION_MATERIAL_MESH)),
                new GlVertexAttributeBinding(ChunkShaderBindingPoints.ATTRIBUTE_COLOR,
                        this.vertexFormat.getAttribute(ChunkMeshAttribute.COLOR_SHADE)),
                new GlVertexAttributeBinding(ChunkShaderBindingPoints.ATTRIBUTE_BLOCK_TEXTURE,
                        this.vertexFormat.getAttribute(ChunkMeshAttribute.BLOCK_TEXTURE)),
                new GlVertexAttributeBinding(ChunkShaderBindingPoints.ATTRIBUTE_LIGHT_TEXTURE,
                        this.vertexFormat.getAttribute(ChunkMeshAttribute.LIGHT_TEXTURE))
        };

        this.commandBufferBuilder = new MultiDrawBatch(ModelQuadFacing.COUNT * RenderRegion.REGION_SIZE);
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

            var batch = this.buildDrawBatches(regionStorage, regionBatch, renderPass, camera);

            if (!batch.isEmpty()) {
                this.setModelMatrixUniforms(shader, region, camera);
                this.executeDrawBatch(commandList, this.createTessellationForRegion(commandList, region, renderPass));
            }
        }

        super.end(renderPass);
    }

    private MultiDrawBatch buildDrawBatches(RenderRegion.RenderRegionStorage storage, ChunkRenderList.RegionBatch batch, TerrainRenderPass pass, ChunkCameraContext camera) {
        var commandBuffer = this.commandBufferBuilder;
        commandBuffer.clear();

        ReversibleArrayIterator<RenderSection> sectionIterator = batch.ordered(pass.isReverseOrder());
        RenderSection section;

        while ((section = sectionIterator.next()) != null) {
            ChunkGraphicsState state = storage.getState(section);

            if (state == null) {
                continue;
            }

            long indexOffset = state.getIndexSegment()
                    .getOffset() * Integer.BYTES;

            int baseVertex = state.getVertexSegment()
                    .getOffset();

            this.addDrawCalls(camera, section, state, indexOffset, baseVertex);
        }

        return commandBuffer;
    }

    private void addDrawCalls(ChunkCameraContext camera, RenderSection section, ChunkGraphicsState state, long indexOffset, int baseVertex) {
        var commandBufferBuilder = this.commandBufferBuilder;

        addDrawCall(commandBufferBuilder, state.getModelPart(ModelQuadFacing.UNASSIGNED), indexOffset, baseVertex);

        if (this.isBlockFaceCullingEnabled) {
            ChunkRenderBounds bounds = section.getBounds();

            if (camera.posY > bounds.minY) {
                addDrawCall(commandBufferBuilder, state.getModelPart(ModelQuadFacing.UP), indexOffset, baseVertex);
            }

            if (camera.posY < bounds.maxY) {
                addDrawCall(commandBufferBuilder, state.getModelPart(ModelQuadFacing.DOWN), indexOffset, baseVertex);
            }

            if (camera.posX > bounds.minX) {
                addDrawCall(commandBufferBuilder, state.getModelPart(ModelQuadFacing.EAST), indexOffset, baseVertex);
            }

            if (camera.posX < bounds.maxX) {
                addDrawCall(commandBufferBuilder, state.getModelPart(ModelQuadFacing.WEST), indexOffset, baseVertex);
            }

            if (camera.posZ > bounds.minZ) {
                addDrawCall(commandBufferBuilder, state.getModelPart(ModelQuadFacing.SOUTH), indexOffset, baseVertex);
            }

            if (camera.posZ < bounds.maxZ) {
                addDrawCall(commandBufferBuilder, state.getModelPart(ModelQuadFacing.NORTH), indexOffset, baseVertex);
            }
        } else {
            for (ModelQuadFacing facing : ModelQuadFacing.DIRECTIONS) {
                addDrawCall(commandBufferBuilder, state.getModelPart(facing), indexOffset, baseVertex);
            }
        }
    }

    private GlTessellation createTessellationForRegion(CommandList commandList, RenderRegion region, TerrainRenderPass pass) {
        var storage = region.getStorage(pass);
        var tessellation = storage.getTessellation();

        if (tessellation == null) {
            storage.setTessellation(tessellation = this.createRegionTessellation(commandList, region));
        }

        return tessellation;
    }

    private void executeDrawBatch(CommandList commandList, GlTessellation tessellation) {
        try (DrawCommandList drawCommandList = commandList.beginTessellating(tessellation)) {
            drawCommandList.multiDrawElementsBaseVertex(this.commandBufferBuilder, GlIndexType.UNSIGNED_INT);
        }
    }

    private void setModelMatrixUniforms(ChunkShaderInterface shader, RenderRegion region, ChunkCameraContext camera) {
        float x = getCameraTranslation(region.getOriginX(), camera.blockX, camera.deltaX);
        float y = getCameraTranslation(region.getOriginY(), camera.blockY, camera.deltaY);
        float z = getCameraTranslation(region.getOriginZ(), camera.blockZ, camera.deltaZ);

        shader.setRegionOffset(x, y, z);
    }

    private static void addDrawCall(MultiDrawBatch batch, ElementRange part, long baseIndexPointer, int baseVertexIndex) {
        if (part != null) {
            batch.add(baseIndexPointer + part.elementPointer(), part.elementCount(), baseVertexIndex);
        }
    }

    private GlTessellation createRegionTessellation(CommandList commandList, RenderRegion region) {
        return commandList.createTessellation(GlPrimitiveType.TRIANGLES, new TessellationBinding[] {
                TessellationBinding.forVertexBuffer(region.vertexBuffers.getBufferObject(), this.vertexAttributeBindings),
                TessellationBinding.forElementBuffer(region.indexBuffers.getBufferObject())
        });
    }

    @Override
    public void delete() {
        super.delete();

        this.commandBufferBuilder.delete();
    }

    private static float getCameraTranslation(int chunkBlockPos, int cameraBlockPos, float cameraPos) {
        return (chunkBlockPos - cameraBlockPos) - cameraPos;
    }
}
